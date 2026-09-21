package com.mplads.model;

/**
 * The analysis values stored for one row of the mplads_ml_results table.
 */
public class MlResult {

    private long id;
    private int riskScore;
    private String riskLevel;
    private double anomalyScore;
    private String recommendation;   // comes from the anomaly_explanation column

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public double getAnomalyScore() { return anomalyScore; }
    public void setAnomalyScore(double anomalyScore) { this.anomalyScore = anomalyScore; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
}