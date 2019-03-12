package com.winjean.service;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.*;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.image.ProcessDiagramGenerator;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipInputStream;

/**
 * @author ：winjean
 * @date ：Created in 2019/2/20 16:52
 * @description：${description}
 * @modified By：
 * @version: $version$
 */

@Api("activiti ")
@Slf4j
@Service
public class ActivitiService {

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private FormService formService;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;

    public Deployment deploy(JSONObject json) throws Exception{
        Model modelData = repositoryService.getModel(json.getString("modelId"));
        byte[] modelEditorSource = repositoryService.getModelEditorSource(modelData.getId());

        ObjectNode modelNode = (ObjectNode) new ObjectMapper().readTree(modelEditorSource);

        BpmnModel model = new BpmnJsonConverter().convertToBpmnModel(modelNode);
        byte[] bpmnBytes = new BpmnXMLConverter().convertToXML(model);

        String processName = modelData.getName() + ".bpmn20.xml";
        Deployment deployment = repositoryService.createDeployment()
                .name(modelData.getName())
                .addString(processName, new String(bpmnBytes, "UTF-8"))
                .deploy();

        log.info("deployment id：{}, deployment name：{}, deployment time：{}", deployment.getId(),deployment.getName(), deployment.getDeploymentTime());

        return deployment;
    }

    public Deployment deploymentWithClasspathResource(JSONObject json){
        Deployment deployment = processEngine.getRepositoryService()//获取流程定义和部署对象相关的Service
                .createDeployment()//创建部署对象
                .name(json.getString("processName"))//声明流程的名称
                .addClasspathResource(json.getString("processFile"))//加载资源文件，一次只能加载一个文件 "activiti/external-form.bpmn"
                .deploy();//完成部署

        log.info("deployment id：{}, deployment name：{}, deployment time：{}", deployment.getId(),deployment.getName(), deployment.getDeploymentTime());

        return deployment;
    }

    public Deployment deploymentWithInputStream(JSONObject json) throws Exception{

        //读取资源作为一个输入流
        FileInputStream bpmnfileInputStream = new FileInputStream(json.getString("bpmnfile"));
        FileInputStream pngfileInputStream = new FileInputStream(json.getString("pngfile"));

        Deployment deployment = processEngine.getRepositoryService()//获取流程定义和部署对象相关的Service
                .createDeployment()//创建部署对象
                .addInputStream("helloworld.bpmn",bpmnfileInputStream)
                .addInputStream("helloworld.png", pngfileInputStream)
                .deploy();//完成部署

        log.info("deployment id：{}, deployment name：{}, deployment time：{}", deployment.getId(),deployment.getName(), deployment.getDeploymentTime());

        return deployment;
    }

    public Deployment deploymentWithString(JSONObject json){
        //字符串
        String text = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><definitions>...</definitions>";

        Deployment deployment = processEngine.getRepositoryService()//获取流程定义和部署对象相关的Service
                .createDeployment()//创建部署对象
                .addString("process-external-form.bpmn",text)
                .deploy();//完成部署

        log.info("deployment id：{}, deployment name：{}, deployment time：{}", deployment.getId(),deployment.getName(), deployment.getDeploymentTime());

        return deployment;
    }

    public Deployment deploymentWithZipInputStream(JSONObject json){
        //从classpath路径下读取资源文件
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("activiti/activiti.zip");
        ZipInputStream zipInputStream = new ZipInputStream(in);
        Deployment deployment = processEngine.getRepositoryService()//获取流程定义和部署对象相关的Service
                .createDeployment()//创建部署对象
                .addZipInputStream(zipInputStream)//使用zip方式部署，将*.bpmn和*.png压缩成zip格式的文件
                .deploy();//完成部署

        log.info("deployment id：{}, deployment name：{}, deployment time：{}", deployment.getId(),deployment.getName(), deployment.getDeploymentTime());

        return deployment;
    }

    public ProcessInstance startProcessByRuntimeService(JSONObject json) {
        identityService.setAuthenticatedUserId(json.getString("authenticatedUserId"));
        Map<String, Object> vars = new HashMap<>(4);
        vars.put("name", "wina");
        vars.put("age", "11");
        vars.put("applyUser", "winjean");
        ProcessDefinition pd =repositoryService.createProcessDefinitionQuery().processDefinitionKey(json.getString("processDefinitionKey")).singleResult();
        ProcessInstance pi = runtimeService.startProcessInstanceById(pd.getId(),vars);
        System.out.println(pi.getId() +" "+ pi.getName());

        log.info("processInstance Id：{}, processInstance name：{}", pi.getId(),pi.getName());

        return pi;
    }

    public ProcessInstance startProcessByFormService(JSONObject json) {
        identityService.setAuthenticatedUserId(json.getString("authenticatedUserId"));
        Map<String, String> vars = new HashMap<>(4);
        vars.put("name", "win");
        vars.put("age", "11");
        vars.put("applyUser", "winjean");
        ProcessDefinition pd =repositoryService.createProcessDefinitionQuery().processDefinitionId(json.getString("processDefinitionId")).singleResult();
        ProcessInstance pi = formService.submitStartFormData(pd.getId(),vars);
        log.info(pi.getId() +" "+ pi.getName());

        return pi;
    }

    public Object completeTaskByTaskService(JSONObject json) {
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(json.getString("processInstanceId")).list();
        Assert.isTrue(tasks.size() > 0," task size should greater than 0 ");

        for(Task task : tasks){

            Map<String, Object> vars = new HashMap<>(4);
            vars.put("p_pass", "true");
            taskService.complete(task.getId(), vars);
            log.info( "[" + task.getName() + "]完成!");
        }

        return json;
    }

    public Object completeTaskByFormService(JSONObject json) {
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(json.getString("processInstanceId")).list();
        Assert.isTrue(tasks.size() > 0," task size should greater than 0. ");

        for(Task task : tasks){
            taskService.unclaim(task.getId());
            taskService.claim(task.getId(),"winjean");
            log.info( "[" + task.getName() + "]签收完成!");

            Map<String, String> vars = new HashMap<>(4);
            vars.put("p_pass", "true");
            formService.submitTaskFormData(task.getId(), vars);
            log.info( "[" + task.getName() + "]完成!");
        }

        return json;
    }

    public Object getFlowElement(JSONObject json){

        //获取BpmnModel对象
        BpmnModel bpmnModel = processEngine.getRepositoryService().getBpmnModel(json.getString("procDefId"));
        //获取Process对象
        Process process = bpmnModel.getProcesses().get(bpmnModel.getProcesses().size()-1);
        //获取所有的FlowElement信息
        Collection<FlowElement> flowElements = process.getFlowElements();

        for(FlowElement flowElement: flowElements){

            if(flowElement instanceof StartEvent){
                StartEvent startEvent = (StartEvent)flowElement;
                log.info("StartEvent: ",  startEvent.getId()+ " " +startEvent.getName());
            } else if (flowElement instanceof org.activiti.bpmn.model.Task){
                org.activiti.bpmn.model.Task task = (org.activiti.bpmn.model.Task)flowElement;
                log.info("Task: ", task.getId()+ " " +task.getName());
            }else if (flowElement instanceof Gateway){
                Gateway gateway = (Gateway)flowElement;
                log.info("Gateway: ", gateway.getId()+ " " +gateway.getName());
            }else if(flowElement instanceof EndEvent){
                EndEvent endEvent = (EndEvent)flowElement;
                log.info("EndEvent: ", endEvent.getId()+ " " +endEvent.getName());
            }else if(flowElement instanceof SequenceFlow){
                SequenceFlow sequenceFlow = (SequenceFlow)flowElement;
                log.info("SequenceFlow: ", sequenceFlow.getId());
            }else if(flowElement instanceof SubProcess){
                SubProcess subProcess = (SubProcess)flowElement;
                log.info("SubProcess: ", subProcess.getId());
            }else{
                log.info("other: ",flowElement.getId()+ " " +flowElement.getName());
            }
        }

        return json;
    }

    public Object getFormInfo(JSONObject json){
        String processDefinitionId = "process-external-form:1:10";

        String startFormKey = formService.getStartFormKey(processDefinitionId);
        Object renderedStartForm = formService.getRenderedStartForm(processDefinitionId);
        log.info("startFormKey:{}, renderedStartForm:{} "+startFormKey,renderedStartForm);


        //正在运行的task的form
        List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().list();
        String taskId = historicTaskInstances.get(0).getId();

        String taskDefinitionKey = historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult().getTaskDefinitionKey();
        String taskFormKey = formService.getTaskFormKey(processDefinitionId,taskDefinitionKey);
        log.info("taskDefinitionKey:{}, taskFormKey:{} ",taskDefinitionKey, taskFormKey);

        Task task = taskService.createTaskQuery().singleResult();
        Object renderedTaskForm = formService.getRenderedTaskForm(task.getId());
        log.info( "renderedTaskForm:{}", renderedTaskForm);

//        StartFormData startFormData = formService.getStartFormData(processDefinitionId);
//        List<FormProperty> formProperties = startFormData.getFormProperties();
//        for(FormProperty formProperty : formProperties){
//            System.out.println(formProperty.getId() +" "+ formProperty.getName() + " " + formProperty.getValue());
//        }

//        TaskFormData taskFormData = formService.getTaskFormData(task.getId());
//        String formKey = taskFormData.getFormKey();
//        List<FormProperty> formProperties = taskFormData.getFormProperties();
//        for(FormProperty formProperty : formProperties){
//            System.out.println(formProperty.getId() +" "+ formProperty.getName() + " " + formProperty.getValue());
//        }

        return json;
    }

    public Object getProcessResourceByProcessDefinitionId(JSONObject json) throws Exception{

        BpmnModel bpmnModel = repositoryService.getBpmnModel(json.getString("processDefinitionId"));

        ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
        InputStream imageStream = diagramGenerator.generateDiagram(
                bpmnModel,"png", new ArrayList<>(), new ArrayList<>(),"宋体","宋体","宋体",null,1.0);
        File file = new File(json.getString("fileName"));
        FileUtils.copyToFile(imageStream,file);

        log.info("image path: {}", file.getPath());

        return json;
    }

    public Object getProcessResourceByProcessInstanceId(JSONObject json) throws Exception{
        String processInstanceId = json.getString("processInstanceId");

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());
        List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstanceId);

        ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
        InputStream imageStream = diagramGenerator.generateDiagram(
                bpmnModel,"png", activeActivityIds, new ArrayList<String>(),"宋体","宋体","宋体",null,1.0);

        File file = new File(json.getString("fileName"));
        FileUtils.copyToFile(imageStream,file);

        log.info("image path: {}", file.getPath());

        return json;
    }

}
