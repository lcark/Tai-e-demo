
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.EmptyParamProvider;
import pascal.taie.analysis.pta.core.solver.EntryPoint;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.util.List;

public class SpringBootHandler implements Plugin {
    private Solver solver;
    private TaintManager manager;


    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        manager = new TaintManager(solver.getHeapModel());
    }

    @Override
    public void onStart() {
        List<JClass> list = this.solver.getHierarchy().allClasses().toList();
        for (JClass jClass : list) {
            boolean flag = false;
            List<Annotation> annotations = jClass.getAnnotations().stream().toList();
            for (Annotation a : annotations) {
                if (a.getType().equals("org.springframework.web.bind.annotation.RestController") || a.getType().equals("org.springframework.stereotype.Controller")) {
                    flag = true;
                    break;
                }
            }
            if (flag) {
                List<JMethod> declaredMethods = jClass.getDeclaredMethods().stream().toList();
                for (JMethod jMethod:declaredMethods){
                    solver.addEntryPoint(new EntryPoint(jMethod, EmptyParamProvider.get()));
                }
            }

        }
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        JMethod method = csMethod.getMethod();
        Context context = csMethod.getContext();
        boolean isMappingMethod = !method.getAnnotations()
                .stream().filter(
                        annotation -> annotation.getType().matches("org.springframework.web.bind.annotation.\\w+Mapping")
                ).toList().isEmpty();
        if(!isMappingMethod){
            return;
        }
        IR ir = method.getIR();
        for (int i = 0; i < ir.getParams().size(); i++) {
            Var param = ir.getParam(i);
            SourcePoint sourcePoint = new ParamSourcePoint(method, new IndexRef(IndexRef.Kind.VAR, i, null));
            Obj taint = manager.makeTaint(sourcePoint, param.getType());
            solver.addVarPointsTo(context, param, taint);
        }
    }
}
