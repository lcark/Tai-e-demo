package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.World;
import pascal.taie.analysis.pta.core.solver.EmptyParamProvider;
import pascal.taie.analysis.pta.core.solver.EntryPoint;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.classes.JClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddMybatisSinkHandler  {


    public static List<Sink>  AddMybatisSink() {
        List<Sink> sinkList = new ArrayList<>();

        List<JClass> list =  World.get().getClassHierarchy().applicationClasses().toList();
        for (JClass jClass : list) {
            if (!jClass.getAnnotations().stream().filter(
                    annotation -> annotation.getType().matches("org.apache.ibatis.annotations.Mapper")
            ).toList().isEmpty()) {
//                System.out.println(jClass);
                jClass.getDeclaredMethods().forEach(jMethod -> {
                    if (!jMethod.getAnnotations().stream().filter(annotation -> annotation.getType().matches("org.apache.ibatis.annotations.Select")).toList().isEmpty()){
                        String valueFromAnnotation = getValueFromAnnotation(jMethod.getAnnotations());
                        if (valueFromAnnotation!=null){
                            if (valueFromAnnotation.contains("$")){
//                                System.out.println(jMethod);
                                Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
                                Matcher matcher = pattern.matcher(valueFromAnnotation);

                                while (matcher.find()) {
                                    String sink = matcher.group(1);
                                    int paramCount = jMethod.getParamCount();
                                    for (int i = 0 ; i< paramCount;i++){
                                        String paramValue = getValueFromAnnotation(jMethod.getParamAnnotations(i));
                                        if (paramValue.contains(sink)){
                                            Sink sink1 = new Sink(jMethod, new IndexRef(IndexRef.Kind.VAR, i,null));
                                            sinkList.add(sink1);
                                        }
                                    }
                                }
                            }
                        }
                    }else {
                        //dela with xml format
                    }
                });
            }

        }
        return sinkList;
    }
    public static String getValueFromAnnotation(Collection<Annotation> annotations) {
        ArrayList<String> value = new ArrayList<>();
        annotations.stream()
                .filter(annotation -> annotation.getType().matches("org.apache.ibatis.annotations..*"))
                .forEach(annotation -> value.add(Objects.requireNonNull(annotation.getElement("value")).toString()));
        return value.size() == 1 ? value.get(0) : null;
    }
}
