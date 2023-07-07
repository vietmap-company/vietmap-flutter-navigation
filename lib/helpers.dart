import 'dart:convert';
import 'dart:io';

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
