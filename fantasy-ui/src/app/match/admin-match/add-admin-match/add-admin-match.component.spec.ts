import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddAdminMatchComponent } from './add-admin-match.component';

describe('AddAdminMatchComponent', () => {
  let component: AddAdminMatchComponent;
  let fixture: ComponentFixture<AddAdminMatchComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AddAdminMatchComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AddAdminMatchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
