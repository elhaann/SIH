package com.mplads.model;

public class Project {

    private int projectId;
    private String mpName;
    private String work;
    private String state;
    private String constituency;
    private String ida;
    private String date;
    private int allocationAmount;
    private String idaApproval;
    private String projectStatus;
    private String house;
    private long mlResultId;   // mplads_ml_results.id (only set for zone rows)

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public String getMpName() { return mpName; }
    public void setMpName(String mpName) { this.mpName = mpName; }

    public String getWork() { return work; }
    public void setWork(String work) { this.work = work; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getConstituency() { return constituency; }
    public void setConstituency(String constituency) { this.constituency = constituency; }

    public String getIda() { return ida; }
    public void setIda(String ida) { this.ida = ida; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getAllocationAmount() { return allocationAmount; }
    public void setAllocationAmount(int allocationAmount) { this.allocationAmount = allocationAmount; }

    public String getIdaApproval() { return idaApproval; }
    public void setIdaApproval(String idaApproval) { this.idaApproval = idaApproval; }

    public String getProjectStatus() { return projectStatus; }
    public void setProjectStatus(String projectStatus) { this.projectStatus = projectStatus; }

    public String getHouse() { return house; }
    public void setHouse(String house) { this.house = house; }

    public long getMlResultId() { return mlResultId; }
    public void setMlResultId(long mlResultId) { this.mlResultId = mlResultId; }
}