package com.example;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.drools.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import com.example.Node;


@PlanningSolution
public class Plan implements Serializable {

    private List<Bus> busList = null;
    private List<Node> nodeList = null;
    private List<School> schoolList = null;
    private List<SourceOrSink> entityList = null;
    private List<Stop> stopList = null;
    private List<Student> studentList = null;

    private HardSoftLongScore score = null;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "entityRange")
    public List<SourceOrSink> getEntityList() { return this.entityList; }
    public void setEntityList(List<SourceOrSink> entityList) { this.entityList = entityList; }

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "busRange")
    public List<Bus> getBusList() { return this.busList; }
    public void setBusList(List<Bus> busList) { this.busList = busList; }

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "nodeRange")
    public List<Node> getNodeList() { return this.nodeList; }
    public void setNodeList(List<Node> nodeList) { this.nodeList = nodeList; }

    public List<School> getSchoolList() { return this.schoolList; }
    public void setSchoolList(List<School> schoolList) { this.schoolList = schoolList; }

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "stopRange")
    public List<Stop> getStopList() { return this.stopList; }
    public void setStopList(List<Stop> stopList) { this.stopList = stopList; }

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "studentRange")
    public List<Student> getStudentList() { return this.studentList; }
    public void setStudentList(List<Student> studentList) { this.studentList = studentList; }

    @PlanningScore
    public HardSoftLongScore getScore() { return score; }
    public void setScore(HardSoftLongScore score) { this.score = score; }

    public void display() {
        // System.out.println("      PREV ←       THIS →       NEXT        BUS");
        // System.out.println("===============================================");
        // for (SourceOrSink entity : entityList) {
        //     System.out.format("%10s ← %10s → %10s %10s\n",
        //                    entity.getPrevious(),
        //                    entity,
        //                    entity.getNext(),
        //                    entity.getBus());
        // }

        System.out.println("\n       BUS →       NEXT");
        System.out.println("=========================");
        for (Bus bus : busList) {
            System.out.format("%10s → %10s\n", bus, bus.getNext());
        }
    }

    public Plan() {
        this.busList = new ArrayList<Bus>();
        this.entityList = new ArrayList<SourceOrSink>();
        this.nodeList = new ArrayList<Node>();
        this.schoolList = new ArrayList<School>();
        this.stopList = new ArrayList<Stop>();
        this.studentList = new ArrayList<Student>();
    }

    public Plan(String csvCostMatrixFile, String csvStudentFile) throws IOException {
        HashSet<String> garageUuids = new HashSet<String>();
        HashSet<String> schoolUuids = new HashSet<String>();
        HashSet<String> stopUuids = new HashSet<String>();
        HashMap<String, Integer> timeMatrix = new HashMap<String, Integer>();
        HashMap<String, Double> distanceMatrix = new HashMap<String, Double>();
        Random rng = new Random(1492);

        this.busList = new ArrayList<Bus>();
        this.entityList = new ArrayList<SourceOrSink>();
        this.nodeList = new ArrayList<Node>();
        this.schoolList = new ArrayList<School>();
        this.stopList = new ArrayList<Stop>();
        this.studentList = new ArrayList<Student>();

        // Build matrices, remember UUIDs
        Reader in = new FileReader(csvCostMatrixFile);
        Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);
        for (CSVRecord record : records) {
            String originId = record.get("origin_id");
            String destinationId = record.get("destination_id");
            String key = originId + destinationId;
            int time = Integer.parseInt(record.get("time"));
            double distance = Double.parseDouble(record.get("distance"));

            if (originId.startsWith("garage_"))
                garageUuids.add(originId);
            else if (originId.startsWith("stop_"))
                stopUuids.add(originId);
            else if (originId.startsWith("school_"))
                schoolUuids.add(originId);

            if (destinationId.startsWith("garage_"))
                garageUuids.add(destinationId);
            else if (destinationId.startsWith("stop_"))
                stopUuids.add(destinationId);
            else if (destinationId.startsWith("school_"))
                schoolUuids.add(destinationId);

            timeMatrix.put(key, time);
            distanceMatrix.put(key, distance);
        }

        // Register cost matrices
        Node.setTimeMatrix(timeMatrix);
        Node.setDistanceMatrix(distanceMatrix);

	// Dummy Bus
        Node dummyNode = new Node("dummy");
        Bus dummyBus = new Bus(dummyNode);
        nodeList.add(dummyNode);
        busList.add(dummyBus);

        // Buses
        for (String uuid : garageUuids) {
            Node node = new Node(uuid);
            Bus bus = new Bus(node);
            nodeList.add(node);
            busList.add(bus);
        }

        // Schools
        for (String uuid : schoolUuids) {
            Node node = new Node(uuid);
            nodeList.add(node);
            for (int i = 0; i < garageUuids.size(); ++i) {
                School school = new School(node);
                schoolList.add(school);
                entityList.add(school);
            }
        }

        // Stops
        for (String uuid : stopUuids) {
            Node node = new Node(uuid);
            nodeList.add(node);
            for (int i = 0; i < schoolUuids.size(); ++i) {
                Stop stop = new Stop(node);
                stopList.add(stop);
                entityList.add(stop);
            }
        }

	// Read student data
	in = new FileReader(csvStudentFile);
	records = CSVFormat.EXCEL.withHeader().parse(in);
	for (CSVRecord record : records) {
	    String firstName = record.get("Student.First.Name");
	    String lastName = record.get("Student.Last.Name");
	    String schoolUuid = "school_" + record.get("School.Code");
	    String stopUuid = "stop_" + record.get("stop_id_cm_reference");
	    Stop stop = null;
	    Node node = null;

	    // Initial solution (this appraoch is unattractive but
	    // temporary).
	    for (Stop _stop : stopList) {
		if (_stop.getNode().getUuid().equals(stopUuid)) {
		    stop = _stop;
		    break;
		}
	    }
	    node = stop.getNode();
	    Student student = new Student(node, firstName, lastName, schoolUuid);
	    studentList.add(student);
	    student.setStop(stop);
	    stop.getStudentList().add(student);
	}

        // Initial solution
        Bus bus = busList.get(0);
        SourceOrSinkOrAnchor previous = bus;
        for (SourceOrSink current : stopList) {
            current.setPrevious(previous);
            current.setBus(bus);
            previous.setNext(current);
            previous = current;
        }
        for (SourceOrSink current : schoolList) {
            current.setPrevious(previous);
            current.setBus(bus);
            previous.setNext(current);
            previous = current;
        }
    }
}
