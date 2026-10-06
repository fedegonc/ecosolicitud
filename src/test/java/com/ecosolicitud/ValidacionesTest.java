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
import jakarta.validation.constraints.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
                // un caso por modo de fallo: vacío, pocos dígitos,
                // solo separadores, letras, demasiado largo
                caso("", false),
                caso("+598 92", false),
                caso("+++++", false),
                caso("abc", false),
                caso("+598 92 000 111 222 333 444 555", false));
    }

    @ParameterizedTest(name = "teléfono «{0}» → válido={1}")
    @MethodSource("telefonos")
    @DisplayName("El teléfono acepta los formatos reales y rechaza lo incompleto")
    void telefono(String valor, boolean valido) {
        var form = new PerfilForm();
        form.setMateriales(List.of(mat("PLASTICO")));
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
        form.setMateriales(List.of(mat("PLASTICO")));

        Allure.step("Validar la dirección ingresada", () ->
                assertThat(invalido(form, "direccion")).isEqualTo(!valido));
    }

    @Test
    @DisplayName("Form y entidad usan la misma regla de teléfono (la base dice lo mismo)")
    void mismaReglaFormYEntidad() throws Exception {
        var patronForm = PerfilForm.class.getDeclaredField("telefono")
                .getAnnotation(Pattern.class).regexp();
        var patronEntidad = Organizacion.class.getDeclaredField("telefono")
                .getAnnotation(Pattern.class).regexp();

        Allure.step("El patrón del form es el de la entidad", () ->
                assertThat(patronForm).isEqualTo(patronEntidad));
    }

    private static Material mat(String codigo) {
        return new Material(codigo, codigo, "botella");
    }

    private boolean invalido(Object form, String campo) {
        return VALIDADOR.validate(form).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals(campo));
    }

    private static Arguments caso(String valor, boolean valido) {
        return Arguments.of(valor, valido);
    }
}
