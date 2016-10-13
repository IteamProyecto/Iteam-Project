package org.iteam.services.team;

import java.util.List;

import org.iteam.data.dal.team.TeamRepositoryImpl;
import org.iteam.data.model.FilterList;
import org.iteam.data.model.Team;
import org.iteam.data.model.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TeamServiceImpl implements TeamService {

	private TeamRepositoryImpl teamRepository;

	@Override
	public boolean putTeam(Team team) {
		return teamRepository.putTeam(team);
	}

	@Override
	public boolean deleteTeam(String ownerName, String teamName) {
		return teamRepository.deleteTeam(ownerName, teamName);
	}

	@Override
	public List<UserDTO> filterToCreateTeam(FilterList filterList) {
		return teamRepository.filterToCreateTeam(filterList);
	}

	@Override
	public List<Team> getTeams(String ownerName) {
		return teamRepository.getTeams(ownerName);
	}

	@Autowired
	private void setTeamRepository(TeamRepositoryImpl teamRepository) {
		this.teamRepository = teamRepository;
	}

	@Override
	public List<String> getTeamByUser(String username) {
		return teamRepository.getTeamByUser(username);
	}
}
