package org.iteam.data.dal.meeting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.iteam.configuration.StringUtilities;
import org.iteam.data.dal.client.ElasticsearchClient;
import org.iteam.data.dal.client.ElasticsearchClientImpl;
import org.iteam.data.dto.Idea;
import org.iteam.data.dto.Meeting;
import org.iteam.data.model.IdeasDTO;
import org.iteam.data.model.MeetingUsers;
import org.iteam.data.model.Reports;
import org.iteam.services.utils.JSONUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class MeetingRepositoryImpl implements MeetingRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeetingRepositoryImpl.class);

    private ElasticsearchClient elasticsearchClientImpl;

    private static final String IDEA_MEETING_ID_FIELD = "meetingId";
    private static final String MEETING_TEAM_NAME_FIELD = "teamName";
    private static final String PROGRAMMED_DATE_FIELD = "programmedDate";
    private static final int MAX_RETRIES = 5;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public boolean createMeeting(Meeting meeting) {
        LOGGER.info("Creating new meeting");
        LOGGER.debug("Meeting: '{}'", meeting.toString());

        meeting.setMeetingId(UUID.randomUUID().toString());
        meeting.setCreationDate(DateTime.now().getMillis());
        DateTime date = new DateTime(meeting.getProgrammedDate());
        meeting.setProgrammedDate(date.withZone(DateTimeZone.UTC).getMillis());
        String data = JSONUtils.ObjectToJSON(meeting);

        IndexResponse response = elasticsearchClientImpl.insertData(data, StringUtilities.INDEX_MEETING,
                StringUtilities.INDEX_TYPE_MEETING, meeting.getMeetingId());

        if(!response.isCreated()) {
            LOGGER.error("The meeting couldn't be created - Meeting: '{}'", meeting.toString());
            return false;
        }

        return true;

    }

    @Override
    public boolean updateMeeting(Meeting updatedMeeting) {
        LOGGER.info("Updating meeting");
        LOGGER.debug("Meeting: '{}'", updatedMeeting.toString());

        if(!ObjectUtils.isEmpty(updatedMeeting.getProgrammedDate())) {
            DateTime date = new DateTime(updatedMeeting.getProgrammedDate());
            updatedMeeting.setProgrammedDate(date.withZone(DateTimeZone.UTC).getMillis());
        }

        String data = JSONUtils.ObjectToJSON(updatedMeeting);

        elasticsearchClientImpl.modifyData(data, StringUtilities.INDEX_MEETING, StringUtilities.INDEX_TYPE_MEETING,
                updatedMeeting.getMeetingId());

        // LOGGER.error("The meeting couldn't be updated - Meeting: '{}'",
        // updatedMeeting.toString());

        return true;
    }

    @Override
    public boolean saveIdeas(IdeasDTO ideas) {

        LOGGER.info("Inserting new ideas");

        LOGGER.debug("Ideas: '{}'", ideas.toString());

        // TODO:check if it's necessary set the insertion date to each idea.
        List<String> dataToInsert = new ArrayList<>();

        ideas.getIdeas().forEach((idea) -> {
            dataToInsert.add(JSONUtils.ObjectToJSON(idea));
        });

        // TODO: we need to make it async, so if the ideas cannot be save we
        // can't delete the index meetingInfo,
        // could we implement a retry politic.
        elasticsearchClientImpl.insertData(dataToInsert, StringUtilities.INDEX_IDEAS, StringUtilities.INDEX_TYPE_IDEAS);

        // TODO: maybe this is not the best way to get the meeting id, try
        // meeting id by parameters.
        elasticsearchClientImpl.delete(StringUtilities.INDEX_MEETING_INFO, StringUtilities.INDEX_TYPE_MEETING_INFO,
                ideas.getIdeas().get(0).getMeetingId());

        return true;
    }

    @Override
    public Reports generateBasicReport(String meetingId, String fieldOrder, SortOrder sortOrder) {
        LOGGER.info("Generating Report by " + fieldOrder);
        LOGGER.debug("Generating report for meeting: '{}'", meetingId);

        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        queryBuilder.must(QueryBuilders.termQuery(IDEA_MEETING_ID_FIELD, meetingId));

        GetResponse meetingResponse = elasticsearchClientImpl.getDocument(StringUtilities.INDEX_MEETING,
                StringUtilities.INDEX_TYPE_MEETING, meetingId);

        Reports report = null;
        List<Idea> ideasList = new ArrayList<>();

        if(meetingResponse.isExists()) {

            try {
                JsonNode meetingNode = OBJECT_MAPPER.readTree(meetingResponse.getSourceAsString());

                SearchResponse response = elasticsearchClientImpl.search(StringUtilities.INDEX_IDEAS, queryBuilder,
                        SortBuilders.fieldSort(fieldOrder).order(sortOrder));

                if(response.getHits().getTotalHits() > 0) {
                    for(SearchHit hit : response.getHits()) {
                        ideasList.add((Idea) JSONUtils.JSONToObject(hit.getSourceAsString(), Idea.class));
                    }
                }

                report = new Reports(meetingNode.at("/topic").asText(), meetingNode.at("/description").asText(),
                        ideasList);

            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return report;
    }

    @Override
    public Reports generateBasicReportByUser(String meetingId) {
        return generateBasicReport(meetingId, "username", SortOrder.ASC);
    }

    @Override
    public Reports generateBasicReportByTag(String meetingId) {
        return generateBasicReport(meetingId, "tag", SortOrder.ASC);
    }

    @Override
    public List<Meeting> getMeetingUser(String username) {
        LOGGER.info("Getting meeting by user: '{}'", username);

        SearchResponse response = elasticsearchClientImpl.search(StringUtilities.INDEX_MEETING,
                QueryBuilders.termQuery(MEETING_TEAM_NAME_FIELD, username),
                SortBuilders.fieldSort(PROGRAMMED_DATE_FIELD).order(SortOrder.ASC));

        List<Meeting> meetingList = new ArrayList<>();

        LOGGER.debug("meetings retrieved: ", response.getHits().getTotalHits());

        for(SearchHit hit : response.getHits()) {

            LOGGER.debug("User '{}' meeting: '{}'", username, hit.getSourceAsString());

            meetingList.add((Meeting) JSONUtils.JSONToObject(hit.getSourceAsString(), Meeting.class));
        }

        LOGGER.debug("User '{}' list of meetings: '{}'", username, meetingList.toString());

        return meetingList;
    }

    @Override
    public List<Meeting> getMeetingByTeamName(List<String> teamName) {
        List<Meeting> meetingList = new ArrayList<>();
        LOGGER.info("Getting meeting for teams: '{}'", teamName.toString());

        SearchResponse response = elasticsearchClientImpl.search(StringUtilities.INDEX_MEETING,
                QueryBuilders.termsQuery(MEETING_TEAM_NAME_FIELD, teamName),
                SortBuilders.fieldSort(PROGRAMMED_DATE_FIELD).order(SortOrder.ASC));

        for(SearchHit hit : response.getHits()) {

            Meeting meeting = (Meeting) JSONUtils.JSONToObject(hit.getSourceAsString(), Meeting.class);
            meetingList.add(meeting);

            LOGGER.debug("Meetings: " + meeting.getMeetingId());
        }

        LOGGER.info("Successfully retrrieve meetings: '{}'", response.getHits().getTotalHits());

        return meetingList;
    }

    @Override
    public void saveMeetingInfo(String data, String meetingId) {
        int count = 0;
        saveInfoRetry(count, data, meetingId);
    }

    private void saveInfoRetry(int count, String data, String meetingId) {
        try {
            elasticsearchClientImpl.insertData(data, StringUtilities.INDEX_MEETING_INFO,
                    StringUtilities.INDEX_TYPE_MEETING_INFO, meetingId);
            LOGGER.info("Meeting info updated - Meeting: '{}'", meetingId);
        } catch (ElasticsearchException e) {
            LOGGER.error("Failed to update meeting info - Retry '{}'", count);
            if(MAX_RETRIES > count) {
                saveInfoRetry(count += 1, data, meetingId);
            }
        }
    }

    @Override
    public String getMeetingInfo(String meetingId) {

        GetResponse response = elasticsearchClientImpl.getDocument(StringUtilities.INDEX_MEETING_INFO,
                StringUtilities.INDEX_TYPE_MEETING_INFO, meetingId);

        if(response.isExists()) {
            return response.getSourceAsString();
        }
        return null;
    }

    @Override
    public void saveMeetingUsers(String users, String meetingId) {
        int count = 0;
        saveUsersRetry(count, users, meetingId);

    }

    private void saveUsersRetry(int count, String users, String meetingId) {
        try {
            elasticsearchClientImpl.insertData(users, StringUtilities.INDEX_MEETING_INFO,
                    StringUtilities.INDEX_TYPE_MEETING_INFO_USERS, meetingId);
            LOGGER.info("Meeting connected users updated - Meeting: '{}'", meetingId);
        } catch (ElasticsearchException e) {
            LOGGER.error("Failed to update meeting connected users - Retry '{}'", count);
            if(MAX_RETRIES > count) {
                saveInfoRetry(count += 1, users, meetingId);
            }
        }
    }

    @Override
    public MeetingUsers getConnectedUsers(String meetingId) {
        MeetingUsers usersList = new MeetingUsers();

        GetResponse response = elasticsearchClientImpl.getDocument(StringUtilities.INDEX_MEETING_INFO,
                StringUtilities.INDEX_TYPE_MEETING_INFO_USERS, meetingId);

        if(response.isExists()) {
            usersList = (MeetingUsers) JSONUtils.JSONToObject(response.getSourceAsString(), MeetingUsers.class);
        }
        return usersList;
    }

    @Override
    public void saveMeetingInfoPBByUser(String meetingId, String info) {

        String username = "";
        String infoToSave = "";
        try {
            JsonNode node = OBJECT_MAPPER.readTree(info);
            username = node.at("/username").asText();
            infoToSave = node.at("/info").toString();
        } catch (IOException e) {
            LOGGER.error("JSON cannot be parsed, personal board ideas were broken");
        }

        elasticsearchClientImpl.insertData(infoToSave, StringUtilities.INDEX_MEETING_INFO, username, meetingId);

        LOGGER.info("Meeting info updated - Meeting: '{}'", String.format("%s_%s", username, meetingId));
    }

    @Override
    public String getMeetingInfoByUserPB(String meetingId, String username) {
        try {
            GetResponse response = elasticsearchClientImpl.getDocument(StringUtilities.INDEX_MEETING_INFO, username,
                    meetingId);

            return response.getSourceAsString();
        } catch (IndexNotFoundException e) {
            LOGGER.warn("No such index yet - for retrieving the persoanl information '{}'",
                    String.format("%s_%s", username, meetingId));
        }

        return null;
    }

    @Autowired
    private void setElasticsearchClientImpl(ElasticsearchClientImpl elasticsearchClientImpl) {
        this.elasticsearchClientImpl = elasticsearchClientImpl;
    }
}
