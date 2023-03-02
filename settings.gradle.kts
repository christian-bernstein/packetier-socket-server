
rootProject.name = "packetier-socket-server"
include("src:test-client")
findProject(":src:test-client")?.name = "test-client"
