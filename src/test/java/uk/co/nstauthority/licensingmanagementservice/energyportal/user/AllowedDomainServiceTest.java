package uk.co.nstauthority.licensingmanagementservice.energyportal.user;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.energyportalapi.generated.types.OrganisationGroupEmailDomain;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupDto;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ExtendWith(MockitoExtension.class)
class AllowedDomainServiceTest {

  private static final String USER_EMAIL = "user@example.com";

  @Mock
  private OrganisationGroupQueryService organisationGroupQueryService;

  @InjectMocks
  private AllowedDomainService allowedDomainService;

  @ParameterizedTest
  @MethodSource("provideDomainIsAllowedCombinations")
  void isAllowedDomain_organisation(String domain, boolean isAllowed) {
    var regTeam = new Team(UUID.randomUUID());
    regTeam.setTeamType(TeamType.ORGANISATION);
    regTeam.setScopeId("123");

    var orgGroup = new OrganisationGroupDto();
    orgGroup.setEmailDomains(List.of(new OrganisationGroupEmailDomain(domain)));

    when(organisationGroupQueryService.getOrganisationGroupById(123))
        .thenReturn(Optional.of(orgGroup));

    assertThat(allowedDomainService.isAllowedDomain(USER_EMAIL, regTeam))
        .isEqualTo(isAllowed);
  }

  @ParameterizedTest
  @MethodSource("provideRegulatorTest")
  void isAllowedDomain_regulatorAndExternalTypes(TeamType type, String domain, boolean isAllowed) {
    var regTeam = new Team(UUID.randomUUID());
    regTeam.setTeamType(type);

    var orgGroup = new OrganisationGroupDto();
    orgGroup.setEmailDomains(List.of(new OrganisationGroupEmailDomain(domain)));

    when(organisationGroupQueryService.getRegulatorOrganisationGroup())
        .thenReturn(Optional.of(orgGroup));

    assertThat(allowedDomainService.isAllowedDomain(USER_EMAIL, regTeam))
        .isEqualTo(isAllowed);
  }

  @ParameterizedTest
  @EnumSource(TeamType.class)
  void isAllowedDomain_ShouldSupportAllTeamTypes(TeamType teamType) {
    Team team = new Team(UUID.randomUUID());
    team.setTeamType(teamType);
    team.setScopeId("1");

    lenient().when(organisationGroupQueryService.getOrganisationGroupById(anyInt()))
        .thenReturn(Optional.empty());
    lenient().when(organisationGroupQueryService.getRegulatorOrganisationGroup())
        .thenReturn(Optional.empty());

    assertDoesNotThrow(() ->
        allowedDomainService.isAllowedDomain(USER_EMAIL, team)
    );
  }

  private static Stream<Arguments> provideDomainIsAllowedCombinations() {
    return Stream.of(
        Arguments.of("example.com", true),
        Arguments.of("domain.com", false)
    );
  }

  private static Stream<Arguments> provideRegulatorTest() {
    List<TeamType> types = List.of(
        TeamType.CARBON_STORAGE_LICENSING,
        TeamType.LICENCE_MANAGEMENT,
        TeamType.OFFSHORE_PRODUCTION_LICENSING,
        TeamType.ONSHORE_PRODUCTION_LICENSING,
        TeamType.REGULATIONS_LICENSING
    );

    return types.stream().flatMap(type ->
        Stream.of(
            Arguments.of(type, "example.com", true),
            Arguments.of(type, "domain.com", false)
        )
    );
  }
}
