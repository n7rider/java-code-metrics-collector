package com.arcticpenguin;

public class MethodMetrics  {

    private int methodLength;

    private AccessModifierType methodAccessModifierType;

    private String methodReturnObject;

    private boolean staticMethod;

    private String methodName;

    public int getMethodLength() {
        return methodLength;
    }

    public void setMethodLength(int methodLength) {
        this.methodLength = methodLength;
    }

    public AccessModifierType getMethodAccessModifierType() {
        return methodAccessModifierType;
    }

    public void setMethodAccessModifierType(AccessModifierType methodAccessModifierType) {
        this.methodAccessModifierType = methodAccessModifierType;
    }

    public String getMethodReturnObject() {
        return methodReturnObject;
    }

    public void setMethodReturnObject(String methodReturnObject) {
        this.methodReturnObject = methodReturnObject;
    }

    public boolean isStaticMethod() {
        return staticMethod;
    }

    public void setStaticMethod(boolean staticMethod) {
        this.staticMethod = staticMethod;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    // TODO GENERATE GETTERS AND SETTERS

}