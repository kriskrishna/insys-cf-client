# Cloud Foundry Client

The cf-java-client project	is	a	Java	language	binding	for	interacting	with	a	Cloud	
Foundry	instance.	The	project	is	broken	up	into	a	number	of	components	which	
expose	different	levels	of	abstraction	depending	on	need.
• cloudfoundry-client – Interfaces,	request,	and	response	objects	mapping	to	
the Cloud	Foundry	REST	APIs.	This	project	has	no	implementation	and	therefore	
cannot	connect	a	Cloud	Foundry	instance	on	its	own.
• cloudfoundry-client-reactor – The	default	implementation	of	the cloudfoundry- client project.	This	implementation	is	based	on	the	Reactor	Netty HttpClient.
• cloudfoundry-operations – An	API	and	implementation	that	corresponds	to	
the Cloud	Foundry	CLI operations.	This	project	builds	on	the cloudfoundry- client and	therefore	has	a	single	implementation.
• cloudfoundry-maven-plugin / cloudfoundry-gradle-plugin – Build	plugins	
for Maven and Gradle.	These	projects	build	on cloudfoundry-operations and	
therefore	have	single	implementations.