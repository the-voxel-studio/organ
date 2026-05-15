package fr.studio.voxel.organ.domain.model

data class Organ(
    val id: Int,
    val name: String,
    val memberIds: List<Int>
)

data class Project(
    val id: Int,
    val name: String,
    val memberIds: List<Int>,
    val organs: List<Organ>
)

data class User(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val projectIdList: List<Int>
)

data class Task(
    val id: Int,
    val name: String,
    val progress: Int,         // X/10
    val deadline: String,
    val projectId : Int,
    val organId : Int
)