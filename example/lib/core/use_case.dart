import 'dart:io';

import 'package:dartz/dartz.dart';

import 'failures/failure.dart';

abstract class UseCase<Type, Params> {
  @override
  Future<Either<Failure, Type>> call(Params params);
}
