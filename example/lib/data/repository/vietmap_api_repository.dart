import '../models/vietmap_place_model.dart';
import '/core/failures/failure.dart';
import '/data/models/vietmap_reverse_model.dart';

import 'package:dartz/dartz.dart';

import '../models/vietmap_autocomplete_model.dart';

abstract class VietmapApiRepository {
  Future<Either<Failure, VietmapReverseModel>> getLocationFromLatLng(
      {required double lat, required double long});

  Future<Either<Failure, List<VietmapAutocompleteModel>>> searchLocation(
      String keySearch);

  Future<Either<Failure, VietmapPlaceModel>> getPlaceDetail(String placeId);
}
