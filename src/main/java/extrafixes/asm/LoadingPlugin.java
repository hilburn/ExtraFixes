package extrafixes.asm;

import com.google.common.eventbus.EventBus;
import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.relauncher.IFMLCallHook;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import java.util.Arrays;
import java.util.Map;

@IFMLLoadingPlugin.TransformerExclusions({"extrafixes.asm."})
public class LoadingPlugin implements IFMLLoadingPlugin
{
    @Override
    public String[] getASMTransformerClass()
    {
        return new String[]{"extrafixes.asm.ExtraFixesTransformer"};
    }

    @Override
    public String getModContainerClass()
    {
        return ExtraFixesDummyContainer.class.getName();
    }

    @Override
    public String getSetupClass()
    {
        return ExtraFixesDummyContainer.class.getName();
    }

    @Override
    public void injectData(Map<String, Object> data)
    {
    }

    @Override
    public String getAccessTransformerClass()
    {
        return null;
    }

    public static class ExtraFixesDummyContainer extends DummyModContainer implements IFMLCallHook
    {
        public ExtraFixesDummyContainer()
        {
            super(new ModMetadata());
            ModMetadata md = getMetadata();
            md.autogenerated = false;
            md.authorList = Arrays.asList("hilburn");
            md.modId = "ExtraFixes";
            md.name = "Extra Fixes";
            md.description = "Fixes some issues with Extra Utilities";
            md.version = "1.2.4b";
        }

        @Override
        public void injectData(Map<String, Object> data)
        {}

        @Override
        public boolean registerBus(EventBus bus, LoadController controller) {

            bus.register(this);
            return true;
        }

        @Override
        public Void call() throws Exception
        {
            return null;
        }
    }
}