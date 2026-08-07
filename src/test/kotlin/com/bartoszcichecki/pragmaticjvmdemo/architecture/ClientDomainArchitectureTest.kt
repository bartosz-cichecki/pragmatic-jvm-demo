package com.bartoszcichecki.pragmaticjvmdemo.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class ClientDomainArchitectureTest {
    @Test
    fun `client domain does not depend on frameworks or outer layers`() {
        val productionClasses =
            ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE)

        noClasses()
            .that()
            .resideInAPackage("$CLIENT_DOMAIN_PACKAGE..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
                "jakarta.persistence..",
                "org.hibernate..",
                "$CLIENT_INFRASTRUCTURE_PACKAGE..",
                "$CLIENT_APPLICATION_PACKAGE..",
                "$CLIENT_UI_PACKAGE..",
            ).check(productionClasses)
    }

    @Test
    fun `persistence types stay inside infrastructure layers`() {
        val productionClasses =
            ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE)

        noClasses()
            .that()
            .resideOutsideOfPackage("..infrastructure..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "jakarta.persistence..",
                "org.hibernate..",
                "org.springframework.data..",
            ).check(productionClasses)
    }

    private companion object {
        const val BASE_PACKAGE = "com.bartoszcichecki.pragmaticjvmdemo"
        const val CLIENT_DOMAIN_PACKAGE = "com.bartoszcichecki.pragmaticjvmdemo.client.domain"
        const val CLIENT_APPLICATION_PACKAGE = "com.bartoszcichecki.pragmaticjvmdemo.client.application"
        const val CLIENT_INFRASTRUCTURE_PACKAGE = "com.bartoszcichecki.pragmaticjvmdemo.client.infrastructure"
        const val CLIENT_UI_PACKAGE = "com.bartoszcichecki.pragmaticjvmdemo.client.ui"
    }
}
