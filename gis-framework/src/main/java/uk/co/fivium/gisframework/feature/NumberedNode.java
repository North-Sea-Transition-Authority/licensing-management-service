package uk.co.fivium.gisframework.feature;

record NumberedNode(
    Line line,
    int displayOrder,
    double x,
    double y,
    boolean ringClosingNode
) {
}
