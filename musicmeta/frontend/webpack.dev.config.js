const BaseWebpack = require('./webpack.base.config');
const Merge = require('webpack-merge');

module.exports = Merge(BaseWebpack, {
  mode: 'development'
});
