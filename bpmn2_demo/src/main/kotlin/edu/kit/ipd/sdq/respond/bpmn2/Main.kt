package edu.kit.ipd.sdq.respond.bpmn2

import org.eclipse.bpmn2.DocumentRoot
import org.eclipse.bpmn2.util.Bpmn2ResourceFactoryImpl
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

fun main() {
    val rs = ResourceSetImpl()
    rs.resourceFactoryRegistry.extensionToFactoryMap["bpmn"] = Bpmn2ResourceFactoryImpl()

    val uri = URI.createFileURI(File(::main.javaClass.classLoader.getResource("example.bpmn")!!.file).absolutePath)
    val bpmnResource = rs.getResource(uri, true)
    val documentRoot = bpmnResource.contents[0] as DocumentRoot

    val startEvents = documentRoot.findStartEvents()
    // Traverse the bpmn graph
    startEvents[0].traverse { println(it.name) }

    //Make changes to the graph and save it
    startEvents[0].name = "Changed"
    bpmnResource.save(FileOutputStream("output.bpmn"), null)
}