const ScalaJS = require('./scalajs.webpack.config');
const Merge = require('webpack-merge');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const path = require('path');
const rootDir = path.resolve(__dirname, '../../../..');
const cssDir = path.resolve(rootDir, 'css');

const WebApp = Merge(ScalaJS, {
  entry: {
    styles: [path.resolve(cssDir, './musicmeta.js')]
  },
  module: {
    rules: [
      {
        test: /\.(png|woff|woff2|eot|ttf|svg)$/,
        type: 'asset/inline'
      },
      {
        test: /\.less$/,
        use: [
          MiniCssExtractPlugin.loader,
          { loader: 'css-loader', options: { importLoaders: 1 } },
          'postcss-loader',
          'less-loader'
        ]
      }
    ]
  },
  plugins: [
    new MiniCssExtractPlugin({filename: '[name].css'})
  ]
});

module.exports = WebApp;
