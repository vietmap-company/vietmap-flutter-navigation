//
//  Debounce.swift
//  vietmap_flutter_navigation
//
//  Created by dev on 22/8/24.
//

import Foundation

class Debouncer {
    private var workItem: DispatchWorkItem?
    private let queue: DispatchQueue
    private let delay: TimeInterval

    init(delay: TimeInterval, queue: DispatchQueue = DispatchQueue.main) {
        self.delay = delay
        self.queue = queue
    }

    func debounce(_ block: @escaping () -> Void) {
        workItem?.cancel()
        
        let work = DispatchWorkItem {
            block()
        }
        workItem = work
        queue.asyncAfter(deadline: .now() + delay, execute: work)
    }
}
