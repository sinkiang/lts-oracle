package com.github.ltsopensource.queue.mysql;

import com.github.ltsopensource.core.cluster.Config;
import com.github.ltsopensource.core.commons.utils.CollectionUtils;
import com.github.ltsopensource.core.json.JSON;
import com.github.ltsopensource.core.logger.Logger;
import com.github.ltsopensource.core.logger.LoggerFactory;
import com.github.ltsopensource.core.support.JobQueueUtils;
import com.github.ltsopensource.queue.JobFeedbackQueue;
import com.github.ltsopensource.queue.domain.JobFeedbackPo;
import com.github.ltsopensource.queue.mysql.support.RshHolder;
import com.github.ltsopensource.store.jdbc.JdbcAbstractAccess;
import com.github.ltsopensource.store.jdbc.builder.*;

import java.util.List;

/**
 * @author Robert HG (254963746@qq.com) on 5/20/15.
 */
public class MysqlJobFeedbackQueue extends JdbcAbstractAccess implements JobFeedbackQueue {
    private static final Logger LOGGER = LoggerFactory.getLogger(MysqlJobFeedbackQueue.class);

    public MysqlJobFeedbackQueue(Config config) {
        super(config);
    }

    @Override
    public boolean createQueue(String jobClientNodeGroup) {
        createTable(readSqlFile("sql/mysql/lts_job_feedback_queue.sql", getTableName(jobClientNodeGroup)));
        return true;
    }

    @Override
    public boolean removeQueue(String jobClientNodeGroup) {
        return new DropTableSql(getSqlTemplate())
                .drop(JobQueueUtils.getFeedbackQueueName(jobClientNodeGroup))
                .doDrop();
    }

    private String getTableName(String jobClientNodeGroup) {
        return JobQueueUtils.getFeedbackQueueName(jobClientNodeGroup);
    }

    @Override
    public boolean add(List<JobFeedbackPo> jobFeedbackPos) {
        LOGGER.debug("[zjj] add, jobFeedbackPos:{} ", JSON.toJSONString(jobFeedbackPos));

        if (CollectionUtils.isEmpty(jobFeedbackPos)) {
            return true;
        }
        // insert ignore duplicate record
        for (JobFeedbackPo jobFeedbackPo : jobFeedbackPos) {
            String jobClientNodeGroup = jobFeedbackPo.getJobRunResult().getJobMeta().getJob().getSubmitNodeGroup();
            new InsertSql(getSqlTemplate())
                    .insert(Delim.MYSQL, getTableName(jobClientNodeGroup))
                    .columns(Delim.MYSQL, "gmt_created", "job_result")
                    .values(jobFeedbackPo.getGmtCreated(), JSON.toJSONString(jobFeedbackPo.getJobRunResult()))
                    .doInsert();
        }
        return true;
    }

    @Override
    public boolean remove(String jobClientNodeGroup, String id) {
        LOGGER.debug("[zjj] remove, id:{}, tablename:{} ", id, getTableName(jobClientNodeGroup));
        return new DeleteSql(getSqlTemplate())
                .delete()
                .from()
                .table(Delim.MYSQL, getTableName(jobClientNodeGroup))
                .where("id = ?", id)
                .doDelete() == 1;
    }

    @Override
    public long getCount(String jobClientNodeGroup) {
        return ((Long) new SelectSql(getSqlTemplate())
                .select()
                .columns("count(1)")
                .from()
                .table(Delim.MYSQL, getTableName(jobClientNodeGroup))
                .single()).intValue();
    }

    @Override
    public List<JobFeedbackPo> fetchTop(String jobClientNodeGroup, int top) {

        return new SelectSql(getSqlTemplate())
                .select()
                .all()
                .from()
                .table(Delim.MYSQL, getTableName(jobClientNodeGroup))
                .orderBy()
                .column(Delim.MYSQL, "gmt_created", OrderByType.ASC)
                .limit(0, top)
                .list(RshHolder.JOB_FEED_BACK_LIST_RSH);
    }


}
