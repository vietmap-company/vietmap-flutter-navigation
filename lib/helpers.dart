import 'dart:convert';
import 'dart:io';

import 'package:flutter/services.dart';

bool isNullOrZero(dynamic val) {
  return val == 0.0 || val == null;
}

Map<String, dynamic> decodeJson({required String data}) {
  Map<String, dynamic> map = {};
  if (Platform.isAndroid) {
    map = jsonDecode(data);
  } else if (Platform.isIOS) {
    map = jsonDecode(jsonDecode(data));
  }
  return map;
}

class VietMapHelper {
  static Future<Uint8List> getBytesFromAsset(String path) async {
    final ByteData bytes = await rootBundle.load(path);
    return bytes.buffer.asUint8List();
  }
}
