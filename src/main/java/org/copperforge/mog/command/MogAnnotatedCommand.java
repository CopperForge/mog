package org.copperforge.mog.command;

public class MogAnnotatedCommand extends MogCommand {

    private String className;

    private String methodName;
    
    public MogAnnotatedCommand() {
        this.setType(MogCommandType.ANNOTATED);
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public String toString() {
        return "MogAnnotatedCommand [className=" + className + ", methodName=" + methodName + ", getName()=" + getName()
                + ", getDescription()=" + getDescription() + ", getType()=" + getType() + "]";
    }

    
}
