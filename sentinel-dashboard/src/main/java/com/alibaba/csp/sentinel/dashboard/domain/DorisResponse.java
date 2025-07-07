package com.alibaba.csp.sentinel.dashboard.domain;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DorisResponse {
    @JsonProperty(value = "TxnId")
    private Long txnId;
    @JsonProperty(value = "Label")
    private String label;
    @JsonProperty(value = "Comment")
    private String comment;
    @JsonProperty(value = "TwoPhaseCommit")
    private String twoPhaseCommit;
    @JsonProperty(value = "Status")
    private String status;
    @JsonProperty(value = "Message")
    private String message;
    @JsonProperty(value = "NumberTotalRows")
    private Integer numberTotalRows;
    @JsonProperty(value = "NumberLoadedRows")
    private Integer numberLoadedRows;
    @JsonProperty(value = "NumberFilteredRows")
    private Integer numberFilteredRows;
    @JsonProperty(value = "NumberUnselectedRows")
    private Integer numberUnselectedRows;
    @JsonProperty(value = "LoadBytes")
    private Long loadBytes;
    @JsonProperty(value = "LoadTimeMs")
    private Long loadTimeMs;
    @JsonProperty(value = "BeginTxnTimeMs")
    private Long beginTxnTimeMs;
    @JsonProperty(value = "StreamLoadPutTimeMs")
    private Long streamLoadPutTimeMs;
    @JsonProperty(value = "ReadDataTimeMs")
    private Long readDataTimeMs;
    @JsonProperty(value = "WriteDataTimeMs")
    private Long writeDataTimeMs;
    @JsonProperty(value = "CommitAndPublishTimeMs")
    private Long commitAndPublishTimeMs;


    public Long getTxnId() {
        return txnId;
    }

    public void setTxnId(Long txnId) {
        this.txnId = txnId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getTwoPhaseCommit() {
        return twoPhaseCommit;
    }

    public void setTwoPhaseCommit(String twoPhaseCommit) {
        this.twoPhaseCommit = twoPhaseCommit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getNumberTotalRows() {
        return numberTotalRows;
    }

    public void setNumberTotalRows(Integer numberTotalRows) {
        this.numberTotalRows = numberTotalRows;
    }

    public Integer getNumberLoadedRows() {
        return numberLoadedRows;
    }

    public void setNumberLoadedRows(Integer numberLoadedRows) {
        this.numberLoadedRows = numberLoadedRows;
    }

    public Integer getNumberFilteredRows() {
        return numberFilteredRows;
    }

    public void setNumberFilteredRows(Integer numberFilteredRows) {
        this.numberFilteredRows = numberFilteredRows;
    }

    public Integer getNumberUnselectedRows() {
        return numberUnselectedRows;
    }

    public void setNumberUnselectedRows(Integer numberUnselectedRows) {
        this.numberUnselectedRows = numberUnselectedRows;
    }

    public Long getLoadBytes() {
        return loadBytes;
    }

    public void setLoadBytes(Long loadBytes) {
        this.loadBytes = loadBytes;
    }

    public Long getLoadTimeMs() {
        return loadTimeMs;
    }

    public void setLoadTimeMs(Long loadTimeMs) {
        this.loadTimeMs = loadTimeMs;
    }

    public Long getBeginTxnTimeMs() {
        return beginTxnTimeMs;
    }

    public void setBeginTxnTimeMs(Long beginTxnTimeMs) {
        this.beginTxnTimeMs = beginTxnTimeMs;
    }

    public Long getStreamLoadPutTimeMs() {
        return streamLoadPutTimeMs;
    }

    public void setStreamLoadPutTimeMs(Long streamLoadPutTimeMs) {
        this.streamLoadPutTimeMs = streamLoadPutTimeMs;
    }

    public Long getReadDataTimeMs() {
        return readDataTimeMs;
    }

    public void setReadDataTimeMs(Long readDataTimeMs) {
        this.readDataTimeMs = readDataTimeMs;
    }

    public Long getWriteDataTimeMs() {
        return writeDataTimeMs;
    }

    public void setWriteDataTimeMs(Long writeDataTimeMs) {
        this.writeDataTimeMs = writeDataTimeMs;
    }

    public Long getCommitAndPublishTimeMs() {
        return commitAndPublishTimeMs;
    }

    public void setCommitAndPublishTimeMs(Long commitAndPublishTimeMs) {
        this.commitAndPublishTimeMs = commitAndPublishTimeMs;
    }
}

