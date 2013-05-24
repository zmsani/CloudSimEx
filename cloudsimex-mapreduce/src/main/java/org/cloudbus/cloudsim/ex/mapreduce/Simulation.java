package org.cloudbus.cloudsim.ex.mapreduce;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Map;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.ex.mapreduce.models.cloud.Cloud;
import org.cloudbus.cloudsim.ex.mapreduce.models.cloud.VmInstance;
import org.cloudbus.cloudsim.ex.mapreduce.models.request.Request;
import org.cloudbus.cloudsim.ex.mapreduce.models.request.Requests;
import org.cloudbus.cloudsim.ex.mapreduce.models.request.Task;
import org.cloudbus.cloudsim.ex.mapreduce.models.request.UserClass;
import org.cloudbus.cloudsim.ex.util.CustomLog;

/**
 * This class contains the main method for execution of the simulation. Here,
 * simulation parameters are defined. Parameters that are dynamic are read from
 * the properties file, whereas other parameters are hardcoded.
 * 
 * Decision on what should be configurable and what is hardcoded was somehow
 * arbitrary. Therefore, if you think that some of the hardcoded values should
 * be customizable, it can be added as a Property. In the Property code there is
 * comments on how to add new properties to the experiment.
 * 
 */
public class Simulation {

    // From Cloud.yaml
    private static Cloud cloud;

    // From Requests.yaml
    private static Requests requests;

    private static MapReduceEngine engine;

    private static java.util.Properties props = new java.util.Properties();

    /**
     * Prints input parameters and execute the simulation a number of times, as
     * defined in the configuration.
     * 
     * @throws Exception
     * 
     */
    public static void main(String[] args) throws Exception {

	Log.printLine("========== Simulation configuration ==========");
	for (Properties property : Properties.values()) {
	    Log.printLine("= " + property + ": " + property.getProperty());
	}
	Log.printLine("==============================================");
	Log.printLine("");

	Experiments experiments = YamlFile.getRequestsFromYaml(Properties.EXPERIMENTS.getProperty());

	try (InputStream is = Files.newInputStream(Paths.get("custom_log.properties"))) {
	    props.load(is);
	}
	CustomLog.configLogger(props);
	// VMs
	CustomLog.redirectToFile("results/vms.csv");
	CustomLog.printHeader(VmInstance.class, ",", new String[] { "experimentNumber", "RequestId", "J", "UserClass",
		"Policy", "CloudDeploymentModel", "Id", "Name", "ExecutionTime", "ExecutionCost", "TasksIdAsString" });
	// TASKs
	CustomLog.redirectToFile("results/tasks.csv");
	CustomLog.printHeader(Task.class, ",", new String[] { "experimentNumber", "RequestId", "J", "UserClass",
		"Policy", "CloudDeploymentModel", "CloudletId", "TaskType", "CloudletLength", "CloudletStatusString",
		"SubmissionTime", "ExecStartTime", "FinalExecTime", "FinishTime", "InstanceVmId", "VmType" });
	// REQUETs
	CustomLog.redirectToFile("results/requests.csv");
	CustomLog.printHeader(Request.class, ",", new String[] { "experimentNumber", "Id", "J", "UserClass", "Policy",
		"CloudDeploymentModel", "Deadline", "Budget", "ExecutionTime", "Cost", "IsDeadlineViolated",
		"IsBudgetViolated", "NumberOfVMs", "LogMessage" });
	// Experiments Plotting
	Experiments.logExperimentsHeader(experiments.experiments.get(0).requests);

	for (int round = 0; round < experiments.experiments.size(); round++) {
	    // BACK TO DEFAULT LOG FILE
	    CustomLog.redirectToFile(props.getProperty("FilePath"), true);
	    CustomLog.printLine("[[[[[[[ Experiment Number: " + round + 1 + " ]]]]]]]");
	    runSimulationRound(round, experiments.experiments.get(round).userClassAllowedPercentage);
	    CustomLog.closeAndRemoveHandlers();
	}
    }

    /**
     * One round of the simulation is executed by this method. Output is printed
     * to the log.
     * 
     */
    private static void runSimulationRound(int experimentNumber, Map<UserClass, Double> userClassAllowedPercentage) {
	Log.printLine("Starting simulation for experiment number: " + (experimentNumber + 1));

	try {

	    // Initialize the CloudSim library
	    CloudSim.init(1, Calendar.getInstance(), false);

	    // Create Broker
	    engine = new MapReduceEngine();
	    engine.currentExperimentRoundNumber = experimentNumber + 1;
	    Cloud.brokerID = engine.getId();

	    // Create datacentres and cloudlets
	    cloud = YamlFile.getCloudFromYaml(Properties.CLOUD.getProperty());
	    cloud.setUserClassAllowedPercentage(userClassAllowedPercentage);
	    engine.setCloud(cloud);
	    Experiments Experiments = YamlFile.getRequestsFromYaml(Properties.EXPERIMENTS.getProperty());

	    requests = Experiments.experiments.get(experimentNumber).requests;

	    int preExperimentIndex = experimentNumber - 1;
	    while (requests.requests.size() == 0 && preExperimentIndex >= 0)
	    {
		requests = Experiments.experiments.get(preExperimentIndex).requests;
		for (Request request : requests.requests)
		{
		    request.policy = Experiments.experiments.get(experimentNumber).policy;
		    request.setCloudDeploymentModel(Experiments.experiments.get(experimentNumber).cloudDeploymentModel);
		}
		preExperimentIndex--;
	    }
	    engine.setRequests(requests);

	    // START
	    CloudSim.startSimulation();
	    engine.logExecutionSummary();

	    Log.printLine("");
	    Log.printLine("");
	} catch (Exception e) {
	    Log.printLine("Unwanted errors happen.");
	    e.printStackTrace();
	} finally {
	    CloudSim.stopSimulation();
	}
    }
}