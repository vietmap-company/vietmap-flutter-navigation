import 'package:flutter/material.dart';

import '../models/constant.dart';
import '../models/route_progress_event.dart';

class BannerInstructionView extends StatelessWidget {
  const BannerInstructionView({
    super.key,
    required this.routeProgressEvent,
    required this.instructionIcon,
  });
  final RouteProgressEvent? routeProgressEvent;
  final Widget instructionIcon;
  @override
  Widget build(BuildContext context) {
    return routeProgressEvent == null
        ? const SizedBox.shrink()
        : Container(
            margin: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(15),
              color: Colors.lightBlue.withOpacity(0.7),
            ),
            height: 100,
            width: MediaQuery.of(context).size.width - 20,
            child: Row(children: [
              const SizedBox(width: 15),
              instructionIcon,
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      routeProgressEvent?.currentStepInstruction ?? '',
                      style: const TextStyle(
                          fontSize: 22,
                          fontWeight: FontWeight.bold,
                          color: Colors.white),
                    ),
                    RichText(
                      text: TextSpan(text: 'Còn ', children: [
                        TextSpan(
                            text: _calculateTotalDistance(
                                routeProgressEvent?.distanceToNextTurn),
                            style: const TextStyle(
                                fontSize: 22,
                                fontWeight: FontWeight.bold,
                                color: Colors.white)),
                        TextSpan(
                            text: _getGuideText(
                                routeProgressEvent?.currentModifier,
                                routeProgressEvent?.currentModifierType),
                            style: const TextStyle(
                                fontSize: 20,
                                fontWeight: FontWeight.bold,
                                color: Colors.white)),
                      ]),
                      maxLines: 2,
                    )
                  ],
                ),
              ),
            ]),
          );
  }

  _calculateTotalDistance(double? distance) {
    var data = distance ?? 0;
    if (data < 1000) return '${data.round()} mét, ';
    return '${(data / 1000).toStringAsFixed(2)} Km, ';
  }

  _getGuideText(String? modifier, String? type) {
    if (modifier != null && type != null) {
      List<String> data = [
        type.replaceAll(' ', '_'),
        modifier.replaceAll(' ', '_')
      ];

      return translationGuide[data.join('_')]?.toLowerCase() ?? '';
    }
    return '';
  }
}
