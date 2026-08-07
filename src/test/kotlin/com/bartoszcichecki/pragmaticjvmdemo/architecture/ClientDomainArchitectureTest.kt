package com.bartoszcichecki.pragmaticjvmdemo.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class ClientDomainArchitectureTest {
    @Test
    fun `client domain does not depend on Spring`() {
        val clientDomainClasses =
            ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(CLIENT_DOMAIN_PACKAGE)

        noClasses()
            .that()
            .resideInAPackage("$CLIENT_DOMAIN_PACKAGE..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..")
            .check(clientDomainClasses)
    }

    private companion object {
        const val CLIENT_DOMAIN_PACKAGE = "com.bartoszcichecki.pragmaticjvmdemo.client.domain"
    }
}
