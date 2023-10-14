package com.annimon.ownlang.parser.ast;

import com.annimon.ownlang.exceptions.UnknownClassException;
import com.annimon.ownlang.lib.*;
import com.annimon.ownlang.util.Range;
import com.annimon.ownlang.util.SourceLocation;
import java.util.Iterator;
import java.util.List;

public final class ObjectCreationExpression implements Node, SourceLocation {
    
    public final String className;
    public final List<Node> constructorArguments;
    private Range range;

    public ObjectCreationExpression(String className, List<Node> constructorArguments) {
        this.className = className;
        this.constructorArguments = constructorArguments;
    }

    public void setRange(Range range) {
        this.range = range;
    }

    @Override
    public Range getRange() {
        return range;
    }
    
    @Override
    public Value eval() {
        final ClassDeclarationStatement cd = ClassDeclarations.get(className);
        if (cd == null) {
            // Is Instantiable?
            if (ScopeHandler.isVariableOrConstantExists(className)) {
                final Value variable = ScopeHandler.getVariableOrConstant(className);
                if (variable instanceof Instantiable instantiable) {
                    return instantiable.newInstance(ctorArgs());
                }
            }
            throw new UnknownClassException(className, range);
        }
        
        // Create an instance and put evaluated fields with method declarations
        final ClassInstanceValue instance = new ClassInstanceValue(className);
        for (AssignmentExpression f : cd.fields) {
            // TODO check only variable assignments
            final String fieldName = ((VariableExpression) f.target).name;
            instance.addField(fieldName, f.eval());
        }
        for (FunctionDefineStatement m : cd.methods) {
            instance.addMethod(m.name, new ClassMethod(m.arguments, m.body, instance, m.getRange()));
        }
        
        // Call a constructor
        instance.callConstructor(ctorArgs());
        return instance;
    }
    
    private Value[] ctorArgs() {
        final int argsSize = constructorArguments.size();
        final Value[] ctorArgs = new Value[argsSize];
        for (int i = 0; i < argsSize; i++) {
            ctorArgs[i] = constructorArguments.get(i).eval();
        }
        return ctorArgs;
    }
    
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public <R, T> R accept(ResultVisitor<R, T> visitor, T t) {
        return visitor.visit(this, t);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("new ").append(className).append(' ');
        final Iterator<Node> it = constructorArguments.iterator();
        if (it.hasNext()) {
            sb.append(it.next());
            while (it.hasNext()) {
                sb.append(", ").append(it.next());
            }
        }
        sb.append(')');
        return sb.toString();
    }
}
