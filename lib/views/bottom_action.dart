import 'package:flutter/material.dart';

import '../embedded/controller.dart';
import '../models/route_progress_event.dart';

class BottomActionView extends StatelessWidget {
  const BottomActionView(
      {super.key,
      this.controller,
      this.routeProgressEvent,
      required this.recenterButton,
      this.onOverviewCallback,
      this.onStopNavigationCallback});
  final MapNavigationViewController? controller;
  final VoidCallback? onOverviewCallback;
  final Widget recenterButton;
  final RouteProgressEvent? routeProgressEvent;
  final VoidCallback? onStopNavigationCallback;

  @override
  Widget build(BuildContext context) {
    return routeProgressEvent == null
        ? const SizedBox.shrink()
        : Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              recenterButton,
              Container(
                height: 100,
                width: MediaQuery.of(context).size.width,
                decoration: const BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.only(
                        topLeft: Radius.circular(15),
                        topRight: Radius.circular(15))),
                child: Row(children: [
                  TextButton(
                      onPressed: () {
                        if (onStopNavigationCallback != null) {
                          onStopNavigationCallback!();
                        }
                        controller?.finishNavigation();
                      },
                      child: Container(
                        height: 50,
                        width: 50,
                        decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: Colors.white,
                            border:
                                Border.all(color: Colors.black45, width: 1)),
                        child: const Icon(
                          Icons.close,
                          color: Colors.black45,
                          size: 30,
                        ),
                      )),
                  Expanded(
                      child: Column(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: [
                      Text(
                        _getTimeArriveRemaining(),
                        style: TextStyle(
                            fontSize: 25,
                            fontWeight: FontWeight.w600,
                            color: Colors.amber[900]),
                      ),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text(
                              _calculateTotalDistance(
                                  routeProgressEvent?.distanceRemaining),
                              style: const TextStyle(
                                  fontSize: 18, color: Colors.black45)),
                          const SizedBox(width: 10),
                          const Icon(
                            Icons.circle_sharp,
                            size: 5,
                          ),
                          const SizedBox(width: 10),
                          Text(_calculateEstimatedArrivalTime(),
                              style: const TextStyle(
                                  fontSize: 18, color: Colors.black45))
                        ],
                      ),
                    ],
                  )),
                  TextButton(
                      onPressed: () {
                        controller?.overview();
                        if (onOverviewCallback != null) {
                          onOverviewCallback!();
                        }
                      },
                      child: Container(
                        height: 50,
                        width: 50,
                        decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: Colors.white,
                            border:
                                Border.all(color: Colors.black45, width: 1)),
                        child: const Icon(
                          Icons.route,
                          color: Colors.black45,
                          size: 30,
                        ),
                      ))
                ]),
              ),
            ],
          );
  }

  _calculateEstimatedArrivalTime() {
    var data = routeProgressEvent?.durationRemaining ?? 0;
    DateTime dateTime = DateTime.now();
    DateTime estimateArriveTime = dateTime.add(Duration(seconds: data.round()));
    // check the time is tomorrow and return the date

    if (estimateArriveTime.day != dateTime.day) {
      // return DateFormat('dd/MM - hh:mm a').format(estimateArriveTime);
      return _formatDate(estimateArriveTime, 'dd/MM - hh:mm a');
    }

    return _formatTime(estimateArriveTime, 'hh:mm a');
  }

  _getTimeArriveRemaining() {
    var data = routeProgressEvent?.durationRemaining ?? 0;
    if (data < 60) return '${data.round()} giây';
    if (data < 3600) return '${(data / 60).round()} phút';
    if (data < 86400) {
      var hour = (data / 3600).floor();

      var minute = ((data - hour * 3600) / 60).round();
      return '${hour < 10 ? '0$hour' : hour} giờ, ${minute < 10 ? '0$minute' : minute} phút';
    }
    var day = (data / 86400).floor();
    var hour = ((data - day * 86400) / 3600).floor();

    var minute = ((data - hour * 3600 - day * 86400) / 60).round();
    return '$day ngày, ${hour < 10 ? '0$hour' : hour} giờ, ${minute < 10 ? '0$minute' : minute} phút';
  }

  _calculateTotalDistance(double? distance) {
    var data = distance ?? 0;
    if (data < 1000) return '${data.round()} mét';
    return '${(data / 1000).toStringAsFixed(2)} Km';
  }

  String _formatDate(DateTime date, String format) {
    String formattedDate = '';
    List<String> pattern = format.split(' ');
    for (int i = 0; i < pattern.length; i++) {
      switch (pattern[i]) {
        case 'dd':
          formattedDate += date.day.toString();
          break;
        case 'MM':
          formattedDate += date.month.toString();
          break;
        case 'hh':
          formattedDate += date.hour.toString();
          break;
        case 'mm':
          formattedDate += date.minute.toString();
          break;
        case 'a':
          formattedDate += date.hour < 12 ? 'am' : 'pm';
          break;
        default:
          break;
      }
    }
    return formattedDate;
  }

  String _formatTime(DateTime time, String format) {
    String formattedTime = '';
    List<String> pattern = format.split(' ');
    for (int i = 0; i < pattern.length; i++) {
      switch (pattern[i]) {
        case 'hh':
          formattedTime += time.hour.toString().padLeft(2, '0');
          break;
        case 'mm':
          formattedTime += time.minute.toString().padLeft(2, '0');
          break;
        case 'a':
          formattedTime += time.hour < 12 ? 'AM' : 'PM';
          break;
        default:
          break;
      }
    }
    return formattedTime;
  }
}
