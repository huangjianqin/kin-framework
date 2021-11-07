package org.kin.framework.ideascript;

/**
 * @author huangjianqin
 * @date 2021/11/7
 */
public class BuilderClassScript {
    private boolean enableABC;

    //getter
    public boolean isEnableABC() {
        return enableABC;
    }

    //--------------------------------------------------builder
    public static Builder builder() {
        return new Builder();
    }

    /** builder **/
    public static class Builder {
        private final BuilderClassScript builderClassScript = new BuilderClassScript();

        public Builder enableABC() {
            builderClassScript.enableABC = true;
            return this;
        }

        public BuilderClassScript build() {
            return builderClassScript;
        }
    }
}
