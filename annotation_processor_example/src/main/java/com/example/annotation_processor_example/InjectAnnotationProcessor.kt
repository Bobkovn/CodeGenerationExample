package com.example.annotation_processor_example

import com.example.annotation_example.Inject
import com.squareup.kotlinpoet.*
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.tools.Diagnostic
import javax.lang.model.type.DeclaredType
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

typealias Injectors = HashMap<TypeName, ArrayList<Pair<String, String>>>

@SupportedAnnotationTypes("com.example.annotation_example.Inject")
class InjectAnnotationProcessor : AbstractProcessor() {
    private val annotation = Inject::class.java
    private val injectors = Injectors()
    private lateinit var options: Map<String?, String?>
    private lateinit var messager: Messager
    private lateinit var elementUtils: Elements

    override fun init(processingEnvironment: ProcessingEnvironment) {
        processingEnvironment.also {
            this.options = it.options
            this.messager = it.messager
            this.elementUtils = it.elementUtils
        }
        super.init(processingEnvironment)
    }

    override fun process(
        annotations: MutableSet<out TypeElement>?,
        env: RoundEnvironment?
    ): Boolean {
        if (annotations?.isEmpty() != false) {
            return false
        }
        var packageName = ""
        val functions = arrayListOf<FunSpec>()
        env?.getElementsAnnotatedWith(annotation)?.forEach { annotatedElement ->
            if (annotatedElement.kind == ElementKind.FIELD) {
                packageName = elementUtils.getPackageOf(annotatedElement).toString()
                val enclosingName = annotatedElement.enclosingElement.simpleName.toString()
                val enclosingTypeName = annotatedElement.enclosingElement.asType().asTypeName()
                val annotatedClassName = annotatedElement.simpleName.toString()
                val type = annotatedElement.asType() as DeclaredType
                val typeName = type.asElement().simpleName
                functions.add(
                    FunSpec.builder(getInjectorFunctionName(annotatedClassName, enclosingName))
                        .returns(type.asTypeName())
                        .addCode(
                            "return $typeName()"
                        ).build()
                )
                var annotatedClassNames = injectors[enclosingTypeName]
                if (annotatedClassNames == null) {
                    annotatedClassNames = arrayListOf()
                }
                annotatedClassNames.add(Pair(annotatedClassName, enclosingName))
                injectors[enclosingTypeName] = annotatedClassNames
            } else {
                messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "${annotatedElement.kind} annotate not supported"
                )
            }
        }
        injectors.forEach {
            val injectMethod = FunSpec.builder("inject")
                .addParameter(
                    INJECT_PARAMETER_NAME,
                    it.key
                )
            it.value.forEach { pairs ->
                injectMethod.addCode(
                    "$INJECT_PARAMETER_NAME.${pairs.first} = ${getInjectorFunctionName(
                        pairs.first,
                        pairs.second
                    )}()\n"
                )
            }
            functions.add(injectMethod.build())
        }
        val generatedClass = TypeSpec.objectBuilder(FILE_NAME)
            .addFunctions(functions)
            .build()
        val generatedFile = FileSpec.builder(packageName, FILE_NAME).addType(generatedClass).build()
        val kaptKotlinGeneratedDir = options[KOTLIN_KAPT_DIR]
        generatedFile.writeTo(File(kaptKotlinGeneratedDir, "$FILE_NAME.kt"))
        return true
    }

    private fun getInjectorFunctionName(annotatedClassName: String, enclosingName: String) =
        "get_${annotatedClassName}_$enclosingName"

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    companion object {
        const val FILE_NAME = "ExampleInjector"
        const val INJECT_PARAMETER_NAME = "param"
        const val KOTLIN_KAPT_DIR = "kapt.kotlin.generated"
    }
}
