package org.copperforge.mog.command;

public class MogAnnotatedCommand extends MogCommand {

    private String className;
    
    public MogAnnotatedCommand() {
        this.setType(MogCommandType.ANNOTATED);
    }

    @Override
    public String toString() {
        return "MogAnnotatedCommand [ " + super.toString() + ", className = " + className + "]";
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    
}
