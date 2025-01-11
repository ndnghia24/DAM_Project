package com.example.orm.processor;

import java.io.PrintWriter;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.example.orm.annotations.Column;
import com.example.orm.annotations.Entity;
import com.google.auto.service.AutoService;

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.example.orm.annotations.Entity")
public class TableGateWayProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Messager messager = processingEnv.getMessager();

        System.out.println("Processing @Entity annotations");

        messager.printMessage(Diagnostic.Kind.NOTE, "Processing @Entity annotations");
        System.out.println("Processing @Entity annotations");

        for (Element element : roundEnv.getElementsAnnotatedWith(Entity.class)) {
            String entityName = element.getSimpleName().toString();

            // Log for debugging
            messager.printMessage(Diagnostic.Kind.NOTE, "Generating gateway for entity: " + entityName);

            // Generate the Gateway class
            try {
                generateGatewayClass(element, entityName);
            } catch (Exception e) {
                System.err.println("Error generating gateway for entity: " + entityName);
                messager.printMessage(Diagnostic.Kind.ERROR, "Error generating gateway for entity: " + entityName);
                e.printStackTrace();
            }
        }
        return true;
    }

    private void generateGatewayClass(Element entityElement, String entityName) throws Exception {
        String packageName = "com.example.orm.gateways";
        String className = entityName + "TableGateway";

        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(packageName + "." + className);

        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            // Generate package and imports
            out.println("package " + packageName + ";");
            out.println();
            out.println("import java.util.List;");
            out.println("import java.util.Optional;");
            out.println("import com.example.orm.repositories.EntityRepository;");
            out.println("import com.example.orm.gateways.ITableGateway;");
            out.println();
            out.println("public class " + className + " implements ITableGateway<" + entityName + "> {");
            out.println("    private final EntityRepository<" + entityName + ", Integer> repository;");
            out.println();
            out.println("    public " + className + "() {");
            out.println("        this.repository = new EntityRepository<>(" + entityName + ".class);");
            out.println("    }");
            out.println();

            // Generate CRUD methods
            generateCrudMethods(out, entityName);

            // Generate `findBy` methods
            generateFindByMethods(out, entityElement, entityName);

            out.println("}");
        }
    }

    private void generateCrudMethods(PrintWriter out, String entityName) {
        out.println("    @Override");
        out.println("    public void insert(" + entityName + " entity) throws Exception {");
        out.println("        repository.save(entity);");
        out.println("    }");
        out.println();

        out.println("    @Override");
        out.println("    public void update(" + entityName + " entity) throws Exception {");
        out.println("        repository.save(entity);");
        out.println("    }");
        out.println();

        out.println("    @Override");
        out.println("    public void delete(int id) throws Exception {");
        out.println("        repository.deleteById(id);");
        out.println("    }");
        out.println();

        out.println("    @Override");
        out.println("    public Optional<" + entityName + "> findById(int id) throws Exception {");
        out.println("        return repository.findById(id);");
        out.println("    }");
        out.println();

        out.println("    @Override");
        out.println("    public List<" + entityName + "> findAll() throws Exception {");
        out.println("        return repository.findAll();");
        out.println("    }");
        out.println();
    }

    private void generateFindByMethods(PrintWriter out, Element entityElement, String entityName) {
        // Loop through the fields in the entity and generate methods for annotated columns
        for (Element field : entityElement.getEnclosedElements()) {
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation != null) {
                String columnName = columnAnnotation.name();
                String methodName = "findBy" + capitalize(field.getSimpleName().toString());
                String fieldType = field.asType().toString();

                out.println("    public List<" + entityName + "> " + methodName + "(" + fieldType + " value) throws Exception {");
                out.println("        return repository.findWithConditions()");
                out.println("                .where(\"" + columnName + "\", value)");
                out.println("                .execute();");
                out.println("    }");
                out.println();
            }
        }
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}