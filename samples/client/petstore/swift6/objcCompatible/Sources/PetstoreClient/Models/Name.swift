//
// Name.swift
//
// Generated by openapi-generator
// https://openapi-generator.tech
//

import Foundation

/** Model for testing model name same as property name */
@objcMembers public class Name: NSObject, Codable, ParameterConvertible {

    public var name: Int
    public var snakeCase: NullEncodable<Int> = .encodeValue(11033)
    public var snakeCaseNum: NSNumber? {
        get {
            if case .encodeValue(let value) = snakeCase {
                return value as NSNumber?
            } else {
                return nil
            }
        }
    }
    public var property: String?
    public var _123number: Int?
    public var _123numberNum: NSNumber? {
        get {
            return _123number as NSNumber?
        }
    }

    public init(name: Int, snakeCase: NullEncodable<Int> = .encodeValue(11033), property: String? = nil, _123number: Int? = nil) {
        self.name = name
        self.snakeCase = snakeCase
        self.property = property
        self._123number = _123number
    }

    public enum CodingKeys: String, CodingKey, CaseIterable {
        case name
        case snakeCase = "snake_case"
        case property
        case _123number = "123Number"
    }

    // Encodable protocol methods

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(name, forKey: .name)
        switch snakeCase {
        case .encodeNothing: break
        case .encodeNull, .encodeValue: try container.encode(snakeCase, forKey: .snakeCase)
        }
        try container.encodeIfPresent(property, forKey: .property)
        try container.encodeIfPresent(_123number, forKey: ._123number)
    }
}

