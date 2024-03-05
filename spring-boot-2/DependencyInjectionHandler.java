package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.World;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignStmt;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DependencyInjectionHandler implements Plugin {
    private Solver solver;
    private boolean isCalled;

    public DependencyInjectionHandler(){
        isCalled = false;
    }

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
    }

//    @Override
//    public void onPhaseFinish() {
//        if(isCalled){
//            return;
//        }
//        isCalled = true;
//
//        List<JField> injectedFields = new ArrayList<>();
//        World.get().getClassHierarchy().allClasses()
//                .map(JClass::getDeclaredFields)
//                .flatMap(Collection::stream)
//                .forEach(field -> {
//                    boolean isInjectedField = field.hasAnnotation("javax.annotation.Resource") ||
//                            field.hasAnnotation("org.springframework.beans.factory.annotation.Autowired") ||
//                            field.hasAnnotation("javax.inject.Inject");
//                    if(isInjectedField){
//                        injectedFields.add(field);
//                    }
//                });
//
//        List<JClass> implementationClasses = new ArrayList<>();
//        implementationClasses.addAll(
//            World.get().getClassHierarchy().allClasses()
//                    .filter(cls -> cls.hasAnnotation("org.springframework.stereotype.Service") ||
//                            cls.hasAnnotation("org.springframework.stereotype.Component")).collect(Collectors.toSet())
//        );
//
//        injectedFields.stream().forEach(field -> {
//            JClass jClass = field.getDeclaringClass();
//
//            Collection<JClass> subClasses = World.get().getClassHierarchy().getAllSubclassesOf(
//                    World.get().getClassHierarchy().getClass(field.getType().getName())
//            );
//            List<JClass> implementors = new ArrayList<>(subClasses);
//            implementors.retainAll(implementationClasses);
//            System.out.printf("%s %s\n", field, implementors);
//
//            Set<CSMethod> csMethodSet = solver.getCallGraph().reachableMethods()
//                    .filter(csMethod -> csMethod.getMethod().getDeclaringClass().equals(jClass))
//                    .collect(Collectors.toSet());
//            csMethodSet.forEach(
//                    csMethod -> {
//                        List<Var> vars = csMethod.getMethod().getIR().getStmts().stream()
//                                .filter(stmt -> stmt instanceof LoadField loadField &&
//                                        loadField.getFieldAccess().getFieldRef().resolve().equals(field))
//                                .map(stmt -> (LoadField) stmt)
//                                .map(AssignStmt::getLValue)
//                                .toList();
//                        implementors.forEach(
//                                implementor -> {
//                                    vars.forEach(
//                                            var -> {
//                                                solver.addPointsTo(solver.getCSManager().getCSVar(csMethod.getContext(), var),
//                                                        solver.getHeapModel().getMockObj(() -> "DEPENDENCY_INJECTION", implementor.getName(), implementor.getType()));
//                                            }
//                                    );
//                                }
//                        );
//                    }
//            );
//        });
//    }

    @Override
    public void onPhaseFinish() {
        if(isCalled){
            return;
        }
        isCalled = true;
        solver.getCallGraph().reachableMethods().forEach(
            csMethod -> {
                if (csMethod.getMethod().getDeclaringClass().getName().matches("^(java\\.|sun\\.|javax\\.|com\\.sun\\.).+$")){
                    return;
                }
                csMethod.getMethod().getIR().getStmts().forEach(
                    stmt -> {
                        if(stmt instanceof Invoke invoke &&
                                (invoke.isVirtual() || invoke.isInterface()) &&
                                invoke.getRValue() instanceof InvokeInstanceExp invokeInstanceExp){
                            Var var = invokeInstanceExp.getBase();
                            Context context = csMethod.getContext();
                            if(solver.getCSManager().getCSVar(context, var).getPointsToSet() != null &&
                                    !solver.getCSManager().getCSVar(context, var).getPointsToSet().isEmpty()) {
                                return;
                            }
                            JClass jClass = World.get().getClassHierarchy().getClass(var.getType().getName());
                            Collection<JClass> implementors = new ArrayList<>();
                            if(invoke.isInterface()){
                                implementors.addAll(World.get().getClassHierarchy().getDirectImplementorsOf(jClass));
                            }else{
                                implementors.add(jClass);
                                implementors.addAll(World.get().getClassHierarchy().getDirectSubclassesOf(jClass));
                            }
                            if(invoke.toString().contains("RequestHttp")){
                                System.out.printf("%s %s %s\n", var, jClass, implementors);
                            }
                            if(implementors.size() <= 3) {
                                implementors.forEach(
                                    implementor -> {
                                        solver.addPointsTo(solver.getCSManager().getCSVar(csMethod.getContext(), var),
                                                solver.getHeapModel().getMockObj(() -> "DEPENDENCY_INJECTION", implementor.getName(), implementor.getType()));
                                    }
                                );
                            }
                        }
                    });
            });
    }
}
