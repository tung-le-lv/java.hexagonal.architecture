package com.acme.orders.bootstrap.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

/**
 * Executable architecture. These rules are the difference between "we follow hexagonal architecture"
 * and a codebase that actually does: a violating import fails the build rather than waiting to be
 * noticed in review.
 *
 * <p>The Maven module graph already makes most of this impossible to compile, but the rules are kept
 * because they also cover what the module graph cannot — package-level direction inside a module, and
 * the absence of framework annotations in the core.
 */
@AnalyzeClasses(
        packages = "com.acme.orders",
        importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    private static final String DOMAIN = "com.acme.orders.domain..";
    private static final String APPLICATION = "com.acme.orders.application..";
    private static final String ADAPTER_REST = "com.acme.orders.adapter.inbound.rest..";
    private static final String ADAPTER_PERSISTENCE = "com.acme.orders.adapter.outbound.persistence..";
    private static final String ADAPTER_MESSAGING = "com.acme.orders.adapter.outbound.messaging..";
    private static final String BOOTSTRAP = "com.acme.orders.bootstrap..";

    // ------------------------------------------------------------------ the hexagon, as a whole

    @ArchTest
    static final ArchRule dependencies_point_inwards = Architectures.layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy(DOMAIN)
            .layer("Application").definedBy(APPLICATION)
            .layer("Adapters").definedBy(ADAPTER_REST, ADAPTER_PERSISTENCE, ADAPTER_MESSAGING)
            .layer("Bootstrap").definedBy(BOOTSTRAP)
            // The domain is depended upon by everything and depends on nothing.
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapters", "Bootstrap")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapters", "Bootstrap")
            // Only the composition root may reach an adapter.
            .whereLayer("Adapters").mayOnlyBeAccessedByLayers("Bootstrap")
            .whereLayer("Bootstrap").mayNotBeAccessedByAnyLayer();

    // ------------------------------------------------------------------ the core must stay pure

    @ArchTest
    static final ArchRule domain_depends_on_nothing_but_the_jdk = classes()
            .that().resideInAPackage(DOMAIN)
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(DOMAIN, "java..", "javax..")
            .because("the hexagon's core must be compilable and testable with no framework present");

    @ArchTest
    static final ArchRule application_depends_only_on_the_domain_and_the_jdk = classes()
            .that().resideInAPackage(APPLICATION)
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(APPLICATION, DOMAIN, "java..", "javax..")
            .because("use cases orchestrate the domain and must not know which technology drives them");

    @ArchTest
    static final ArchRule core_is_free_of_framework_annotations = noClasses()
            .that().resideInAnyPackage(DOMAIN, APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "com.fasterxml..", "tools.jackson..", "org.hibernate..", "org.slf4j..")
            .because("an annotation from a framework is a dependency on that framework");

    @ArchTest
    static final ArchRule domain_does_not_know_about_persistence_or_transport = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..adapter..", "..repository..", "..entity..", "..dto..", "..jpa..", "..rest..")
            .because("persistence and transport are details the model must be able to outlive");

    // ------------------------------------------------------------------ adapters are interchangeable

    // One rule per adapter: ArchUnit's target predicate cannot see which adapter the dependency came
    // from, so "different adapter" has to be spelled out pairwise. Being explicit also means a
    // violation names the exact pair that went wrong.

    @ArchTest
    static final ArchRule rest_adapter_is_independent_of_the_others = noClasses()
            .that().resideInAPackage(ADAPTER_REST)
            .should().dependOnClassesThat().resideInAnyPackage(ADAPTER_PERSISTENCE, ADAPTER_MESSAGING)
            .because("how orders are stored or published must not matter to how they arrive over HTTP");

    @ArchTest
    static final ArchRule persistence_adapter_is_independent_of_the_others = noClasses()
            .that().resideInAPackage(ADAPTER_PERSISTENCE)
            .should().dependOnClassesThat().resideInAnyPackage(ADAPTER_REST, ADAPTER_MESSAGING)
            .because("the store must be replaceable without touching transport or messaging");

    @ArchTest
    static final ArchRule messaging_adapter_is_independent_of_the_others = noClasses()
            .that().resideInAPackage(ADAPTER_MESSAGING)
            .should().dependOnClassesThat().resideInAnyPackage(ADAPTER_REST, ADAPTER_PERSISTENCE)
            .because("the outbox relay reaches the stored events through a port, not through the JPA adapter");

    @ArchTest
    static final ArchRule only_persistence_uses_jpa = noClasses()
            .that().resideOutsideOfPackages(ADAPTER_PERSISTENCE, BOOTSTRAP)
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.hibernate..")
            .because("if JPA types escape the persistence adapter, the database is no longer replaceable");

    @ArchTest
    static final ArchRule only_rest_adapter_uses_web_types = noClasses()
            .that().resideOutsideOfPackages(ADAPTER_REST, BOOTSTRAP)
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.web..", "org.springframework.http..")
            .because("HTTP is one way in, not the application's vocabulary");

    @ArchTest
    static final ArchRule spring_data_stays_behind_the_port = noClasses()
            // Bootstrap is exempt: enabling repositories is exactly the composition root's job.
            .that().resideOutsideOfPackages(ADAPTER_PERSISTENCE, BOOTSTRAP)
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.data..")
            .because("Page and Pageable leaking through a port would couple every caller to Spring Data");

    // ------------------------------------------------------------------ port discipline

    @ArchTest
    static final ArchRule ports_are_interfaces = classes()
            .that().resideInAnyPackage("com.acme.orders.application.port..")
            .and().haveSimpleNameNotEndingWith("Command")
            .and().haveSimpleNameNotEndingWith("Message")
            .should().beInterfaces()
            .because("a port is a contract; a class in that position is an implementation in disguise");

    @ArchTest
    static final ArchRule adapters_never_call_use_case_implementations_directly = noClasses()
            .that().resideInAPackage("com.acme.orders.adapter..")
            .should().dependOnClassesThat().resideInAPackage("com.acme.orders.application.usecase..")
            .because("adapters must be wired to ports, so an implementation can be replaced or decorated");

    @ArchTest
    static final ArchRule no_cycles_between_slices = com.tngtech.archunit.library.dependencies.SlicesRuleDefinition
            .slices().matching("com.acme.orders.(*)..").should().beFreeOfCycles();
}
