package extrafixes.asm;

import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.classloading.FMLForgePlugin;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtraFixesTransformer implements IClassTransformer
{
    private enum MethodName
    {
        GENERATOR_ENERGY_HANDLER("transmitEnergy","()V"){
            @Override
            public void replace(InsnList list)
            {
                AbstractInsnNode node = list.getFirst();
                while (node.getNext()!=null)
                {
                    if (node instanceof TypeInsnNode)
                    {
                        TypeInsnNode insnNode = (TypeInsnNode)node;
                        if (insnNode.desc.equals("cofh/api/energy/IEnergyHandler")) insnNode.desc = "cofh/api/energy/IEnergyReceiver";
                    }
                    if (node instanceof MethodInsnNode && node.getOpcode() == Opcodes.INVOKEINTERFACE)
                    {
                        if (((MethodInsnNode)node).owner.equals("cofh/api/energy/IEnergyHandler")) ((MethodInsnNode)node).owner = "cofh/api/energy/IEnergyReceiver";
                    }
                    node = node.getNext();
                }
            }

            @Override
            public boolean shouldReplace()
            {
                return true;
            }
        },
        GENERATOR_TRANSMIT_RATE("getStorage","()Lcofh/api/energy/EnergyStorage;"){
            {
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                instructions.add(new FieldInsnNode(Opcodes.GETFIELD,"com/rwtema/extrautils/tileentity/generator/TileEntityGenerator","storage","Lcofh/api/energy/EnergyStorage;"));
                instructions.add(new LdcInsnNode(100000));
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "com/rwtema/extrautils/tileentity/generator/TileEntityGenerator", "getMultiplier", "()I", false));
                instructions.add(new InsnNode(Opcodes.IMUL));
                instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "cofh/api/energy/EnergyStorage", "setMaxTransfer", "(I)V", false));
            }

            @Override
            public AbstractInsnNode getInjectionPoint(InsnList list)
            {
                AbstractInsnNode node = list.getLast();
                while(node.getPrevious()!=null)
                {
                    if (node instanceof MethodInsnNode && ((MethodInsnNode)node).owner.equals("cofh/api/energy/EnergyStorage") && ((MethodInsnNode)node).name.equals("setCapacity")) return node.getNext();
                    node = node.getPrevious();
                }
                return super.getInjectionPoint(list);
            }
        },
        ADD_ASPECT_RECIPE("addAspectRecipe", "(Lnet/minecraft/item/ItemStack;[Ljava/lang/Object;)V")
                {
                    @Override
                    public AbstractInsnNode getInjectionPoint(InsnList list)
                    {
                        AbstractInsnNode node = list.getFirst();
                        LabelNode labelNode = null;
                        AbstractInsnNode IFNONNULL = null;
                        while (node != null && !((node = node.getNext()).getOpcode() == Opcodes.RETURN))
                        {
                            if (node instanceof LabelNode) labelNode = (LabelNode)node;
                            else if (node.getOpcode() == Opcodes.IFNONNULL) IFNONNULL = node;
                        }
                        list.insertBefore(IFNONNULL, new JumpInsnNode(Opcodes.IFNULL, labelNode));
                        list.insertBefore(IFNONNULL, new VarInsnNode(Opcodes.ALOAD, 0));
                        list.insertBefore(IFNONNULL, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/item/ItemStack", FMLForgePlugin.RUNTIME_DEOBF? "func_77973_b" : "getItem", "()Lnet/minecraft/item/Item;", false));
                        return node;
                    }

                    @Override
                    public boolean shouldInject()
                    {
                        return true;
                    }
                };

        private String deObf;
        private String args;
        public InsnList instructions = new InsnList();

        MethodName(String name, String args)
        {
            deObf = name;
            this.args = args;
        }

        static {
        }

        public AbstractInsnNode getInjectionPoint(InsnList list)
        {
            return null;
        }

        public void replace(InsnList list){}

        public boolean shouldReplace()
        {
            return false;
        }

        public boolean shouldInject()
        {
            return instructions.size()>0;
        }

        public String getName()
        {
            return deObf;
        }

        public String getArgs()
        {
            return args;
        }
    }

    private enum ClassName
    {
        TE_GENERATOR("com.rwtema.extrautils.tileentity.generator.TileEntityGenerator", MethodName.GENERATOR_ENERGY_HANDLER, MethodName.GENERATOR_TRANSMIT_RATE),
        THAUMCRAFT_HELPER("com.rwtema.extrautils.ThaumcraftHelper", MethodName.ADD_ASPECT_RECIPE);
        private String deObf;
        private MethodName[] methods;


        ClassName(String name, MethodName... methods)
        {
            deObf = name;
            this.methods = methods;
        }

        public String getName()
        {
            return deObf;
        }

        public MethodName[] getMethods()
        {
            return methods;
        }
    }

    private static Map<String,ClassName> classMap = new HashMap<String, ClassName>();

    static
    {
        for (ClassName className: ClassName.values()) classMap.put(className.getName(),className);
    }

    @Override
    public byte[] transform(String className, String className2, byte[] bytes)
    {
        ClassName clazz = classMap.get(className);
        if (clazz!=null)
        {
            for (MethodName method: clazz.getMethods())
            {
                if (method.shouldInject()) bytes = inject(method,bytes);
                if (method.shouldReplace()) bytes = replace(method, bytes);
            }
            classMap.remove(className);
        }

        return bytes;
    }

    private byte[] inject(MethodName methodName, byte[] data)
    {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(data);
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES);

        MethodNode methodNode = getMethodByName(classNode, methodName);
        AbstractInsnNode node = methodName.getInjectionPoint(methodNode.instructions);
        methodNode.instructions.insertBefore(node,methodName.instructions);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private byte[] replace(MethodName methodName, byte[] data)
    {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(data);
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES);

        MethodNode methodNode = getMethodByName(classNode, methodName);
        methodName.replace(methodNode.instructions);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    public static MethodNode getMethodByName(ClassNode classNode, MethodName obfName) {
        List<MethodNode> methods = classNode.methods;
        for (int k = 0; k < methods.size(); k++) {
            MethodNode method = methods.get(k);
            if (method.name.equals(obfName.getName()) && method.desc.equals(obfName.getArgs())) {
                return method;
            }
        }
        return classNode.methods.get(0);
    }
}
