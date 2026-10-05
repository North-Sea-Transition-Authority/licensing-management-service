package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerMapping;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AbstractInterceptorRuleTest;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.InterceptorRuleTestEndpoints;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRoleTestUtil;

@ExtendWith(MockitoExtension.class)
class InvokingUserHasCorrectorRoleForCorrectionInterceptorRuleTest extends AbstractInterceptorRuleTest {

  @Mock
  private LicenceCorrectionService licenceCorrectionService;

  @Mock
  private UserDetailService userDetailService;

  @Mock
  private TeamQueryService teamQueryService;

  @InjectMocks
  private InvokingUserHasCorrectorRoleForCorrectionInterceptorRule rule;

  private final ServiceUserDetail user = ServiceUserDetailTestUtil.newBuilder().build();

  @Test
  void supports() {
    assertThat(rule.supports()).isEqualTo(InvokingUserHasCorrectorRoleForCorrection.class);
  }

  @ParameterizedTest
  @MethodSource("matchingCorrectorRoleParams")
  void check_whenUserHasCorrectorRoleForLicenceType_thenContinueAsNormal(
      LicenceType licenceType,
      Role role
  ) throws NoSuchMethodException {
    var correction = buildCorrection(licenceType);
    givenCorrectionExists(correction);
    givenUserHasRoles(Set.of(teamRole(role)));

    var interceptorResult = rule.check(getAnnotation(), request, response);

    assertThat(interceptorResult).isEqualTo(SecurityRuleResult.continueAsNormal());
    verify(request).setAttribute("validatedCorrection", correction);
    verifyNoInteractions(response);
  }

  private static Stream<Arguments> matchingCorrectorRoleParams() {
    return Stream.of(
        Arguments.of(LicenceType.SEAWARD_PRODUCTION, Role.PRODUCTION_LICENCE_CORRECTOR),
        Arguments.of(LicenceType.LANDWARD_PRODUCTION, Role.PRODUCTION_LICENCE_CORRECTOR),
        Arguments.of(LicenceType.CARBON_STORAGE, Role.CARBON_STORAGE_LICENCE_CORRECTOR)
    );
  }

  @ParameterizedTest
  @MethodSource("mismatchedCorrectorRoleParams")
  void check_whenUserOnlyHasCorrectorRoleForOtherLicenceType_thenForbidden(
      LicenceType licenceType,
      Role role
  ) throws NoSuchMethodException {
    var correction = buildCorrection(licenceType);
    givenCorrectionExists(correction);
    givenUserHasRoles(Set.of(teamRole(role)));

    var interceptorResult = rule.check(getAnnotation(), request, response);

    assertThat(interceptorResult).isEqualTo(expectedForbiddenResult(correction));
    verify(request, never()).setAttribute("validatedCorrection", correction);
  }

  private static Stream<Arguments> mismatchedCorrectorRoleParams() {
    return Stream.of(
        Arguments.of(LicenceType.SEAWARD_PRODUCTION, Role.CARBON_STORAGE_LICENCE_CORRECTOR),
        Arguments.of(LicenceType.CARBON_STORAGE, Role.PRODUCTION_LICENCE_CORRECTOR)
    );
  }

  @Test
  void check_whenUserHasNoRoles_thenForbidden() throws NoSuchMethodException {
    var correction = buildCorrection(LicenceType.SEAWARD_PRODUCTION);
    givenCorrectionExists(correction);
    givenUserHasRoles(Set.of());

    var interceptorResult = rule.check(getAnnotation(), request, response);

    assertThat(interceptorResult).isEqualTo(expectedForbiddenResult(correction));
    verify(request, never()).setAttribute("validatedCorrection", correction);
  }

  @Test
  void check_whenLicenceTypeHasNoCorrectorRole_thenForbidden() throws NoSuchMethodException {
    var correction = buildCorrection(LicenceType.GAS_STORAGE);
    givenCorrectionExists(correction);
    givenUserHasRoles(Set.of(
        teamRole(Role.PRODUCTION_LICENCE_CORRECTOR),
        teamRole(Role.CARBON_STORAGE_LICENCE_CORRECTOR)
    ));

    var interceptorResult = rule.check(getAnnotation(), request, response);

    assertThat(interceptorResult).isEqualTo(expectedForbiddenResult(correction));
    verify(request, never()).setAttribute("validatedCorrection", correction);
  }

  @Test
  void check_whenCorrectionNotFound_thenNotFound() throws NoSuchMethodException {
    var correctionId = UUID.randomUUID();
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of("correctionId", correctionId.toString()));
    when(licenceCorrectionService.findById(correctionId)).thenReturn(Optional.empty());

    var interceptorResult = rule.check(getAnnotation(), request, response);

    assertThat(interceptorResult).isEqualTo(SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.NOT_FOUND,
        "Licence correction %s not found".formatted(correctionId)
    ));
    verifyNoInteractions(teamQueryService);
  }

  private LicenceCorrection buildCorrection(LicenceType licenceType) {
    return LicenceCorrectionTestUtil.newBuilder()
        .withLicence(LicenceTestUtil.builder().withLicenceType(licenceType).build())
        .build();
  }

  private void givenCorrectionExists(LicenceCorrection correction) {
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of("correctionId", correction.getId().toString()));
    when(licenceCorrectionService.findById(correction.getId())).thenReturn(Optional.of(correction));
  }

  private void givenUserHasRoles(Set<TeamRole> teamRoles) {
    when(userDetailService.getUserDetail()).thenReturn(user);
    when(teamQueryService.getTeamRolesForUser(user.wuaId())).thenReturn(teamRoles);
  }

  private TeamRole teamRole(Role role) {
    return TeamRoleTestUtil.newBuilder()
        .withRole(role)
        .withWuaId(user.wuaId())
        .build();
  }

  private SecurityRuleResult expectedForbiddenResult(LicenceCorrection correction) {
    return SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.FORBIDDEN,
        "User with wuaId %s does not have the corrector role for licence correction %s"
            .formatted(user.wuaId(), correction.getId())
    );
  }

  private InvokingUserHasCorrectorRoleForCorrection getAnnotation() throws NoSuchMethodException {
    return getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("invokingUserHasCorrectorRoleForCorrection", UUID.class),
        InvokingUserHasCorrectorRoleForCorrection.class
    );
  }
}