import { TestBed } from '@angular/core/testing';
import { Paymentservice } from './paymentservice';


describe('Paymentservice', () => {
  let service: Paymentservice;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Paymentservice);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
