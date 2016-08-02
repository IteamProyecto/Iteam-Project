package org.iteam.data.dal.meeting;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.iteam.configuration.StringUtilities;
import org.iteam.data.dal.client.ElasticsearchClient;
import org.iteam.data.dal.client.ElasticsearchClientImpl;
import org.iteam.data.model.IdeasDTO;
import org.iteam.data.model.Meeting;
import org.iteam.services.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class MeetingRepositoryImpl implements MeetingRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeetingRepositoryImpl.class);

    private ElasticsearchClient elasticsearchClientImpl;

    private static final String IDEA_MEETING_ID_FIELD = "meetingId";
    private static final String RANKING_ID_FIELD = "ranking";

    @Override
    public boolean createMeeting(Meeting meeting) {
        LOGGER.info("Creating new meeting");
        LOGGER.debug("Meeting: '{}'", meeting.toString());

        String data = JSONUtils.ObjectToJSON(meeting);

        IndexResponse response = elasticsearchClientImpl.insertData(data, StringUtilities.INDEX_MEETING,
                StringUtilities.INDEX_TYPE_MEETING);

        if(!response.isCreated()) {
            LOGGER.error("The meeting couldn't be created - Meeting: '{}'", meeting.toString());
            return false;
        }
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

        BulkResponse response = elasticsearchClientImpl.insertData(dataToInsert, StringUtilities.INDEX_IDEAS,
                StringUtilities.INDEX_TYPE_IDEAS);

        if(response.hasFailures()) {

            LOGGER.error("Ideas bulk insertion has failed - Error: '{}'", response.buildFailureMessage());
            return false;
        }

        return true;
    }

    @Override
    public void generateBasicReport(String meetingId) {
        LOGGER.info("Generating Report");
        LOGGER.debug("Generating report for meeting: '{}'", meetingId);

        // TODO: check this in another iterations if there will be more filters.
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        queryBuilder.must(QueryBuilders.termQuery(IDEA_MEETING_ID_FIELD, meetingId));

        SearchResponse response = elasticsearchClientImpl.search(StringUtilities.INDEX_IDEAS, queryBuilder,
                SortBuilders.fieldSort(RANKING_ID_FIELD).order(SortOrder.ASC));

        if(response != null && response.getHits().getTotalHits() > 0) {
            // TODO: make the file writer, generate file in the path given by
            // configuration
            PrintWriter writer = null;
            try {
                // FIXME: change this code please!!!!
                writer = new PrintWriter(new FileWriter(String.format("%s/%s", "C:\\ideas", meetingId), true));
                writer.append("*****First Report*****");
                writer.append("\nIdeas order by ranking");

                for(SearchHit hit : response.getHits()) {
                    writer.append("\n" + hit.getSourceAsString());
                }
                writer.close();

            } catch (IOException e) {
                LOGGER.error("The report couldn't be processed - Error:", e);
            } finally {
                if(writer != null) {
                    writer.close();
                }
            }
        }

    }

    @Autowired
    private void setElasticsearchClientImpl(ElasticsearchClientImpl elasticsearchClientImpl) {
        this.elasticsearchClientImpl = elasticsearchClientImpl;
    }

}
