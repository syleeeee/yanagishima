export const SITENAME = 'yanagishima'

export const VERSION = '18.0'

export const LINKS = {
  aboutThis: 'https://github.com/yanagishima/yanagishima/blob/master/README.md',
  bugsFeedback: 'https://github.com/yanagishima/yanagishima/issues',
  mailAdmin: ''
}

export const TABS = [
  {
    id: 'treeview',
    icon: 'sitemap',
    name: 'Treeview'
  },
  {
    id: 'qlist',
    icon: 'tasks',
    name: 'Query List'
  },
  {
    id: 'history',
    icon: 'history',
    name: 'History'
  },
  {
    id: 'bookmark',
    icon: 'star',
    name: 'Bookmark'
  },
  {
    id: 'result',
    icon: 'table',
    name: 'Result'
  },
  {
    id: 'timeline',
    icon: 'clock',
    iconStyle: 'far',
    name: 'Timeline'
  }
]

export const HASH_KEYS = [
  // [key, required]
  ['datasource', true],
  ['engine', true],
  ['tab', true],
  ['queryid', false],
  ['bookmark_id', false],
  ['chart', false],
  ['pivot', false],
  ['line', false],
  ['table', false]
]

export const HIDDEN_QUERY_PREFIX = '/* yanagishima */'

export const DATE_COLUMN_NAMES = [
  'dt',
  'yyyymmdd',
  'log_date'
]

export const CHART_TYPES = {
  1: {
    name: 'Line Chart',
    type: 'LineChart',
    minRows: 2,
    option: {}
  },
  2: {
    name: 'Stacked Area Chart',
    type: 'AreaChart',
    minRows: 3,
    option: {
      isStacked: true
    }
  },
  3: {
    name: 'Full-Stacked Area Chart',
    type: 'AreaChart',
    minRows: 3,
    option: {
      isStacked: 'relative'
    }
  },
  4: {
    name: 'Column Chart',
    type: 'ColumnChart',
    minRows: 2,
    option: {
      isStacked: false
    }
  },
  5: {
    name: 'Stacked Column Chart',
    type: 'ColumnChart',
    minRows: 3,
    option: {
      isStacked: true
    }
  }
}

export const CHART_OPTIONS = {
  width: '100%',
  height: 360,
  fontName: 'Droid Sans',
  fontSize: 12,
  chartArea: {
    width: '80%'
  },
  legend: {
    position: 'bottom',
    textStyle: {
      fontName: 'Droid Sans',
      fontSize: 12
    }
  },
  tooltip: {
    textStyle: {
      fontName: 'Droid Sans',
      fontSize: 12
    }
  },
  vAxis: {
    minValue: 0,
    gridlines: {
      color: '#eee'
    },
    titleTextStyle: {
      italic: false
    }
  },
  hAxis: {
    gridlines: {
      color: '#eee'
    },
    titleTextStyle: {
      italic: false
    }
  }
}

export const COMPLETE_LIST = {
  snippet: [
    "WHERE dt='{yesterday}' LIMIT 100",
    "WHERE yyyymmdd='{yesterday}' LIMIT 100",
    "WHERE log_date='{yesterday}' LIMIT 100"
  ],
  function: [
    'abs',
    'acos',
    'approx_distinct',
    'approx_percentile',
    'approx_set',
    'arbitrary',
    'arrays_overlap',
    'array_agg',
    'array_distinct',
    'array_except',
    'array_intersect',
    'array_join',
    'array_max',
    'array_min',
    'array_position',
    'array_remove',
    'array_sort',
    'array_union',
    'asin',
    'atan',
    'atan2',
    'avg',
    'bar',
    'bing_tile',
    'bing_tile_at',
    'bing_tile_coordinates',
    'bing_tile_polygon',
    'bing_tile_quadkey',
    'bing_tile_zoom_level',
    'bitwise_and',
    'bitwise_and_agg',
    'bitwise_not',
    'bitwise_or',
    'bitwise_or_agg',
    'bitwise_xor',
    'bit_count',
    'bool_and',
    'bool_or',
    'cardinality',
    'cbrt',
    'ceil',
    'ceiling',
    'char2hexint',
    'checksum',
    'chr',
    'classify',
    'coalesce',
    'codepoint',
    'color',
    'concat',
    'contains',
    'corr',
    'cos',
    'cosh',
    'cosine_similarity',
    'count',
    'count_if',
    'covar_pop',
    'covar_samp',
    'crc32',
    'cume_dist',
    'current_date',
    'current_time',
    'current_timestamp',
    'current_timezone',
    'date',
    'date_add',
    'date_diff',
    'date_format',
    'date_parse',
    'date_trunc',
    'day',
    'day_of_month',
    'day_of_week',
    'day_of_year',
    'degrees',
    'dense_rank',
    'dow',
    'doy',
    'e',
    'element_at',
    'empty_approx_set',
    'evaluate_classifier_predictions',
    'every',
    'exp',
    'features',
    'filter',
    'first_value',
    'flatten',
    'floor',
    'format_datetime',
    'from_base',
    'from_base64',
    'from_base64url',
    'from_big_endian_64',
    'from_hex',
    'from_iso8601_date',
    'from_iso8601_timestamp',
    'from_unixtime',
    'from_utf8',
    'Function',
    'geometric_mean',
    'geometry_to_bing_tiles',
    'greatest',
    'hamming_distance',
    'hash_counts',
    'histogram',
    'hour',
    'index',
    'infinity',
    'intersection_cardinality',
    'inverse_normal_cdf',
    'is_appliance',
    'is_crawler',
    'is_finite',
    'is_infinite',
    'is_json_scalar',
    'is_misc',
    'is_mobilephone',
    'is_nan',
    'is_pc',
    'is_smartphone',
    'is_unknown',
    'jaccard_index',
    'json_array_contains',
    'json_array_get',
    'json_array_length',
    'json_extract',
    'json_extract_scalar',
    'json_format',
    'json_parse',
    'json_size',
    'kurtosis',
    'lag',
    'last_value',
    'lead',
    'learn_classifier',
    'learn_libsvm_classifier',
    'learn_libsvm_regressor',
    'learn_regressor',
    'least',
    'length',
    'levenshtein_distance',
    'like_pattern',
    'ln',
    'localtime',
    'localtimestamp',
    'log',
    'log10',
    'log2',
    'lower',
    'lpad',
    'ltrim',
    'make_set_digest',
    'map',
    'map_agg',
    'map_concat',
    'map_entries',
    'map_filter',
    'map_from_entries',
    'map_keys',
    'map_union',
    'map_values',
    'map_zip_with',
    'max',
    'max_by',
    'md5',
    'merge',
    'merge_set_digest',
    'min',
    'minute',
    'min_by',
    'mod',
    'month',
    'multimap_agg',
    'nan',
    'normalize',
    'normal_cdf',
    'now',
    'nth_value',
    'ntile',
    'numeric_histogram',
    'objectid',
    'parse_agent',
    'parse_datetime',
    'parse_duration',
    'percent_rank',
    'pi',
    'pow',
    'power',
    'quarter',
    'radians',
    'rand',
    'random',
    'rank',
    'reduce',
    'regexp_extract',
    'regexp_extract_all',
    'regexp_like',
    'regexp_replace',
    'regexp_split',
    'regress',
    'regr_intercept',
    'regr_slope',
    'render',
    'repeat',
    'replace',
    'reverse',
    'rgb',
    'round',
    'row_number',
    'rpad',
    'rtrim',
    'second',
    'sequence',
    'sha1',
    'sha256',
    'sha512',
    'shuffle',
    'sign',
    'sin',
    'skewness',
    'slice',
    'split',
    'split_part',
    'split_to_map',
    'sqrt',
    'stddev',
    'stddev_pop',
    'stddev_samp',
    'strpos',
    'ST_Area',
    'ST_AsText',
    'ST_Boundary',
    'ST_Buffer',
    'ST_Centroid',
    'ST_Contains',
    'ST_CoordDim',
    'ST_Crosses',
    'ST_Difference',
    'ST_Dimension',
    'ST_Disjoint',
    'ST_Distance',
    'ST_EndPoint',
    'ST_Envelope',
    'ST_Equals',
    'ST_ExteriorRing',
    'ST_GeometryFromText',
    'ST_Intersection',
    'ST_Intersects',
    'ST_IsClosed',
    'ST_IsEmpty',
    'ST_IsRing',
    'ST_Length',
    'ST_LineFromText',
    'ST_NumInteriorRing',
    'ST_NumPoints',
    'ST_Overlaps',
    'ST_Point',
    'ST_Polygon',
    'ST_Relate',
    'ST_StartPoint',
    'ST_SymDifference',
    'ST_Touches',
    'ST_Within',
    'ST_X',
    'ST_XMax',
    'ST_XMin',
    'ST_Y',
    'ST_YMax',
    'ST_YMin',
    'substr',
    'substring',
    'sum',
    'tan',
    'tanh',
    'timezone_hour',
    'timezone_minute',
    'to_base',
    'to_base64',
    'to_base64url',
    'to_big_endian_64',
    'to_char',
    'to_date',
    'to_hex',
    'to_ieee754_32',
    'to_ieee754_64',
    'to_iso8601',
    'to_milliseconds',
    'to_timestamp',
    'to_unixtime',
    'to_utf8',
    'transform',
    'transform_keys',
    'transform_values',
    'trim',
    'truncate',
    'typeof',
    'upper',
    'url_decode',
    'url_encode',
    'url_extract_fragment',
    'url_extract_host',
    'url_extract_parameter',
    'url_extract_path',
    'url_extract_port',
    'url_extract_protocol',
    'url_extract_query',
    'uuid',
    'variance',
    'var_pop',
    'var_samp',
    'week',
    'week_of_year',
    'width_bucket',
    'word_stem',
    'xxhash64',
    'year',
    'year_of_week',
    'yow',
    'zip',
    'zip_with'
  ]
}