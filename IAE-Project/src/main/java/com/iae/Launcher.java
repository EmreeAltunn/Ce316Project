package com.iae;

/**
 * Fat JAR giris noktasi.
 *
 * <p>JavaFX {@code Application} alt sinifi (bkz. {@link Main}) bir fat JAR'in
 * {@code Main-Class}'i olarak dogrudan calistirilamaz — "JavaFX runtime
 * components are missing" hatasi verir. Bu sinif {@code Application}'i extend
 * etmedigi icin sorunsuz baslar ve uygulamayi {@link Main#main(String[])}
 * uzerinden devreye sokar.
 *
 * <p>maven-shade-plugin manifest'inde {@code Main-Class = com.iae.Launcher}.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Main.main(args);
    }
}
