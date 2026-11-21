package dev.pvpmacro.config;

public final class PvpMacroConfig {
    public int configVersion = 1;

    /** Master switch for every macro. */
    public boolean masterEnabled = true;

    /** Show short action-bar messages (macro refused / cancelled). */
    public boolean feedback = true;

    public AttributeSwapConfig attributeSwap = new AttributeSwapConfig();

    public void sanitize() {
        if (attributeSwap == null) {
            attributeSwap = new AttributeSwapConfig();
        }
        attributeSwap.sanitize();
    }
}
