package net.micropact.aea.utility;

/**
 * Enum containing the Script Object Language Types that entellitrak supports.
 *
 * @author zmiller
 */
public enum ScriptObjectLanguageType {
    /**
     * Beanshell.
     */
    BEANSHELL(1, "Beanshell"),
    /**
     * Javascript.
     */
    JAVASCRIPT(2, "Javascript"),
    /**
     * Java.
     */
    JAVA(3, "Java"),
    /**
     * Groovy.
     */
    GROOVY(4, "Groovy"),
    /**
     * HTML.
     */
    HTML(5, "HTML"),
    /**
     * SQL.
     */
    SQL(6, "SQL"),
    /**
     * CSS.
     * */
    CSS(8, "CSS");

    /**
     * The id which entellitrak internally uses to represent the Script Object Language.
     */
    private final int id;

    /**
     * A user-readable name used to identify the language.
     */
    private final String name;


    /**
     * Constructor.
     *
     * @param scriptObjectLanguageTypeId The id which entellitrak internally uses to represent
     *      the Script Object Language.
     * @param scriptObjectLanguageName A user-readable name used to identify the language.
     */
    ScriptObjectLanguageType(final int scriptObjectLanguageTypeId, final String scriptObjectLanguageName){
        id = scriptObjectLanguageTypeId;
        name = scriptObjectLanguageName;
    }

    /**
     * Get the id entellitrak internally uses to represent the Script Object language.
     *
     * @return The id entellitrak internally uses to represent the Script Object Language.
     */
    public int getId(){
        return id;
    }

    /**
     * Get the name entellitrak uses to represent the Scirpt Object Language Type.
     *
     * @return The name entellitrak uses to represent the Script Object Language Type.
     */
    public String getName(){
        return name;
    }

    /**
     * Returns the ScriptObjectLanguageType given the id that entellitrak uses to reference it.
     *
     * @param id The id entellitrak internally uses to represent the Script Object Language.
     * @return TheObjectLanguageType corresponding to the given entellitrak id.
     */
    public static ScriptObjectLanguageType getById(final int id){
        for(final ScriptObjectLanguageType languageType : values()){
            if(id == languageType.getId()){
                return languageType;
            }
        }
        throw new IllegalArgumentException(String.format("Could not find ScriptObjectLanguageType for id: %s", id));
    }

    /**
     * Returns the ScriptObjectLanguageType given the id that entellitrak uses to reference it.
     *
     * @param id The id entellitrak internally uses to represent the Script Object Language.
     * @return TheObjectLanguageType corresponding to the given entellitrak id.
     */
    public static ScriptObjectLanguageType getById(final Number id){
        if (id == null) {
            return null;
        }

        for(final ScriptObjectLanguageType languageType : values()){
            if(id.intValue() == languageType.getId()){
                return languageType;
            }
        }
        throw new IllegalArgumentException(String.format("Could not find ScriptObjectLanguageType for id: %s", id));
    }
}
