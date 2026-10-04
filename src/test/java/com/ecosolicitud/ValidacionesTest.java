package com.ecosolicitud;

import java.util.List;
import java.util.stream.Stream;

import com.ecosolicitud.organizacion.interno.Organizacion;
import com.ecosolicitud.organizacion.interno.PerfilForm;
import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Material;
import com.ecosolicitud.solicitud.interno.NuevaForm;

import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

// Las reglas de validación se prueban con casos que DEBEN pasar y casos que DEBEN fallar.
// Un test con un solo caso feliz valida que el código corre, no que la regla sea correcta:
// así vivió el patrón del teléfono que rechazaba "+598 92 000-111" y aceptaba "+++++".
// Se valida contra el Validator real (todos los constraints juntos), no contra una copia del patrón.
@Epic("Calidad de las reglas")
@Feature("Las validaciones aceptan lo real y rechazan lo que no")
class ValidacionesTest {

    private static final Validator VALIDADOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    static Stream<Arguments> telefonos() {
        return Stream.of(
                // lo que la gente escribe de verdad: debe pasar
                caso("+598 92 000 111", true),
                caso("+55 55 9000 0000", true),
                caso("+598 92 000-111", true),
                caso("(598) 9200 0111", true),
                caso("092.000.111", true),
                caso("092 000 111", true),
                // incompleto o basura: debe fallar
                caso("", false),
                caso("   ", false),
                caso("+598", false),
                caso("+5", false),
                caso("+598 92", false),
                caso("092", false),
                caso("+++++", false),
                caso("9 9 9", false),
                caso("  123", false),
                caso("abc", false),
                caso("+598 92 000 111 222 333 444 555", false));
    }

    @ParameterizedTest(name = "teléfono «{0}» → válido={1}")
    @MethodSource("telefonos")
    @DisplayName("El teléfono acepta los formatos reales y rechaza lo incompleto")
    void telefono(String valor, boolean valido) {
        var form = new PerfilForm();
        form.setMateriales(List.of(Material.PLASTICO));
        form.setHorario("Lun a Vie 8 a 17 h");
        form.setTelefono(valor);

        Allure.step("Validar el teléfono ingresado", () ->
                assertThat(invalido(form, "telefono")).isEqualTo(!valido));
    }

    static Stream<Arguments> direcciones() {
        return Stream.of(
                caso("Av. Italia 1234", true),
                caso("x".repeat(120), true),
                caso("", false),
                caso("   ", false),
                caso("x".repeat(121), false));
    }

    @ParameterizedTest(name = "dirección de {0} caracteres → válida={1}")
    @MethodSource("direcciones")
    @DisplayName("La dirección exige 1 a 120 caracteres (FE-01)")
    void direccion(String valor, boolean valido) {
        var form = new NuevaForm();
        form.setDireccion(valor);
        form.setOrganizacionId("frontera-limpia");
        form.setMateriales(List.of(Material.PLASTICO));

        Allure.step("Validar la dirección ingresada", () ->
                assertThat(invalido(form, "direccion")).isEqualTo(!valido));
    }

    @ParameterizedTest(name = "teléfono en entidad «{0}» → válido={1}")
    @MethodSource("telefonos")
    @DisplayName("La entidad exige la misma regla que el form (la base dice lo mismo)")
    void telefonoEnEntidad(String valor, boolean valido) {
        var org = new Organizacion("o", "Org", Ciudad.RIVERA,
                List.of(Material.PLASTICO), "Lun a Vie 8 a 17 h", valor);

        Allure.step("Validar el teléfono en la entidad", () ->
                assertThat(invalido(org, "telefono")).isEqualTo(!valido));
    }

    private boolean invalido(Object form, String campo) {
        return VALIDADOR.validate(form).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals(campo));
    }

    private static Arguments caso(String valor, boolean valido) {
        return Arguments.of(valor, valido);
    }
}
