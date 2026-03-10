package person.wangchen11.planet.metadata

import person.wangchen11.planet.i18n.LocalizationManager

object MetadataText {
    fun buildingName(model: BuildingModel): String = LocalizationManager.tr("building.${model.id}.name", model.name)
    fun buildingDescription(model: BuildingModel): String = LocalizationManager.tr("building.${model.id}.description", model.description)

    fun cropName(model: CropModel): String = LocalizationManager.tr("crop.${model.id}.name", model.name)
    fun cropDescription(model: CropModel): String = LocalizationManager.tr("crop.${model.id}.description", model.description)

    fun enemyName(model: EnemyModel): String = LocalizationManager.tr("enemy.${model.id}.name", model.name)
    fun enemyDescription(model: EnemyModel): String = LocalizationManager.tr("enemy.${model.id}.description", model.description)

    fun techName(model: TechModel): String = LocalizationManager.tr("tech.${model.id}.name", model.name)
    fun techDescription(model: TechModel): String = LocalizationManager.tr("tech.${model.id}.description", model.description)

    fun techBranchName(model: TechBranchModel): String = LocalizationManager.tr("techBranch.${model.id}.name", model.name)
    fun techBranchDescription(model: TechBranchModel): String = LocalizationManager.tr("techBranch.${model.id}.description", model.description)

    fun terrainName(model: TerrainModel): String = LocalizationManager.tr("terrain.${model.id}.name", model.name)
    fun terrainDescription(model: TerrainModel): String = LocalizationManager.tr("terrain.${model.id}.description", model.description)

    fun resourceName(model: ResourceModel): String = LocalizationManager.tr("resource.${model.id}.name", model.name)
    fun resourceDescription(model: ResourceModel): String = LocalizationManager.tr("resource.${model.id}.description", model.description)
}
