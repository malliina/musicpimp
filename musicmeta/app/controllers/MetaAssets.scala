package controllers

import controllers.Assets.Asset

class MetaAssets(builder: AssetsBuilder) {
  def versioned(path: String, file: Asset) = builder.versioned(path, file)
}
