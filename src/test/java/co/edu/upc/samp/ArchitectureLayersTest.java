package co.edu.upc.samp;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Verifica automáticamente que se respete la arquitectura de 4 capas exigida en la rúbrica:
 * {@code presentacion} solo llama a {@code servicios}, {@code servicios} a {@code datos} y
 * {@code entidades} permanece independiente de las capas superiores.
 */
class ArchitectureLayersTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("co.edu.upc.samp");

    @Test
    void presentationShouldNotAccessDataLayerDirectly() {
        noClasses()
                .that().resideInAPackage("..presentacion..")
                .should().dependOnClassesThat().resideInAPackage("..datos..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void entitiesShouldNotDependOnUpperLayers() {
        noClasses()
                .that().resideInAPackage("..entidades..")
                .should().dependOnClassesThat().resideInAnyPackage("..datos..", "..servicios..", "..presentacion..")
                .check(classes);
    }

    @Test
    void dataLayerShouldNotDependOnServicesOrPresentation() {
        noClasses()
                .that().resideInAPackage("..datos..")
                .should().dependOnClassesThat().resideInAnyPackage("..servicios..", "..presentacion..")
                .check(classes);
    }
}
